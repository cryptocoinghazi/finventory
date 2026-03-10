"use client"

import * as React from "react"
import { Check, ChevronsUpDown } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import { apiFetch } from "@/lib/api"
import { useDebounce } from "@/hooks/use-debounce"

interface SmartSelectProps<T> {
  value?: string
  onSelect: (value: string, item?: T) => void
  placeholder?: string
  searchPlaceholder?: string
  endpoint?: string
  options?: T[]
  labelKey: keyof T
  valueKey: keyof T
  renderOption?: (item: T) => React.ReactNode
  renderValue?: (item: T) => React.ReactNode
  filterOption?: (item: T, query: string) => boolean
  onCreate?: (query: string) => void
  createLabel?: (query: string) => string
  disabled?: boolean
  className?: string
  initialLabel?: string
}

export function SmartSelect<T>({
  value,
  onSelect,
  placeholder = "Select...",
  searchPlaceholder = "Search...",
  endpoint,
  options: optionsProp,
  labelKey,
  valueKey,
  renderOption,
  renderValue,
  filterOption,
  onCreate,
  createLabel,
  disabled = false,
  className,
  initialLabel,
}: SmartSelectProps<T>) {
  const [open, setOpen] = React.useState(false)
  const [query, setQuery] = React.useState("")
  const [remoteOptions, setRemoteOptions] = React.useState<T[]>([])
  const [loading, setLoading] = React.useState(false)
  
  const debouncedQuery = useDebounce(query, 300)

  React.useEffect(() => {
    if (!open) return
    if (optionsProp) return
    if (!endpoint) return

    let active = true
    setLoading(true)

    const fetchOptions = async () => {
      try {
        const url = debouncedQuery 
          ? `${endpoint}${endpoint.includes('?') ? '&' : '?'}search=${encodeURIComponent(debouncedQuery)}`
          : endpoint
        
        const res = await apiFetch(url)
        if (!res.ok) throw new Error("Failed to fetch")
        
        const data = await res.json()
        if (active) {
          setRemoteOptions(Array.isArray(data) ? data : data.content || [])
        }
      } catch (err) {
        console.error("SmartSelect fetch error:", err)
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchOptions()

    return () => {
      active = false
    }
  }, [open, debouncedQuery, endpoint, optionsProp])

  const options = optionsProp ?? remoteOptions

  const visibleOptions = React.useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return options
    return options.filter((opt) => {
      if (filterOption) return filterOption(opt, q)
      const label = String((opt as Record<string, unknown>)[String(labelKey)] ?? "")
      return label.toLowerCase().includes(q)
    })
  }, [filterOption, labelKey, options, query])

  const canCreate = React.useMemo(() => {
    const q = query.trim()
    return Boolean(onCreate && q.length > 0 && !loading && visibleOptions.length === 0)
  }, [loading, onCreate, query, visibleOptions.length])

  const selectedOption = React.useMemo(() => {
    const found = options.find((opt) => String((opt as Record<string, unknown>)[String(valueKey)]) === value)
    return found
  }, [options, value, valueKey])

  // If value exists but option not found in current list (e.g. initial load), 
  // we might need to fetch the single item. 
  // For now, we rely on the parent to pass initial data or just show ID if not found.
  // Ideally, the endpoint should support fetching by ID or we fetch all on mount.
  
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className={cn("w-full justify-between", className)}
          disabled={disabled}
        >
          {value
            ? selectedOption
              ? (renderValue
                  ? renderValue(selectedOption)
                  : String((selectedOption as Record<string, unknown>)[String(labelKey)]))
              : initialLabel || "Selected"
            : placeholder}
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[300px] p-0" align="start">
        <Command shouldFilter={false}> 
          <CommandInput 
            placeholder={searchPlaceholder} 
            value={query}
            onValueChange={setQuery}
          />
          <CommandList>
            {loading && <div className="py-6 text-center text-sm text-muted-foreground">Loading...</div>}
            {canCreate ? (
              <CommandItem
                value="__create__"
                onSelect={() => {
                  const q = query.trim()
                  if (!q) return
                  onCreate?.(q)
                  setOpen(false)
                  setQuery("")
                }}
              >
                {createLabel ? createLabel(query.trim()) : `Add "${query.trim()}"`}
              </CommandItem>
            ) : null}
            {!loading && !canCreate && visibleOptions.length === 0 ? (
              <CommandEmpty>No results found.</CommandEmpty>
            ) : null}
            {!loading && visibleOptions.map((item) => {
              const itemValue = String((item as Record<string, unknown>)[String(valueKey)])
              const itemLabel = String((item as Record<string, unknown>)[String(labelKey)])
              return (
                <CommandItem
                  key={itemValue}
                  value={itemValue}
                  onSelect={() => {
                    onSelect(itemValue, item)
                    setOpen(false)
                  }}
                >
                  <Check
                    className={cn(
                      "mr-2 h-4 w-4",
                      value === itemValue ? "opacity-100" : "opacity-0"
                    )}
                  />
                  {renderOption ? renderOption(item) : itemLabel}
                </CommandItem>
              )
            })}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  )
}
