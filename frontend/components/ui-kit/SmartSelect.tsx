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
  endpoint: string
  labelKey: keyof T
  valueKey: keyof T
  renderOption?: (item: T) => React.ReactNode
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
  labelKey,
  valueKey,
  renderOption,
  disabled = false,
  className,
  initialLabel,
}: SmartSelectProps<T>) {
  const [open, setOpen] = React.useState(false)
  const [query, setQuery] = React.useState("")
  const [options, setOptions] = React.useState<T[]>([])
  const [loading, setLoading] = React.useState(false)
  
  const debouncedQuery = useDebounce(query, 300)

  // Fetch initial options or search
  React.useEffect(() => {
    if (!open) return

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
          setOptions(Array.isArray(data) ? data : data.content || []) // Handle array or Page<T>
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
  }, [open, debouncedQuery, endpoint])

  const selectedOption = React.useMemo(() => {
    // If we have options loaded, try to find in them
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
              ? (String((selectedOption as Record<string, unknown>)[String(labelKey)]))
              : initialLabel || "Selected" // Fallback if we can't find label
            : placeholder}
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[300px] p-0" align="start">
        <Command shouldFilter={false}> 
          {/* We handle filtering server-side or manually */}
          <CommandInput 
            placeholder={searchPlaceholder} 
            value={query}
            onValueChange={setQuery}
          />
          <CommandList>
            {loading && <div className="py-6 text-center text-sm text-muted-foreground">Loading...</div>}
            {!loading && options.length === 0 && (
              <CommandEmpty>No results found.</CommandEmpty>
            )}
            {!loading && options.map((item) => {
              const itemValue = String((item as Record<string, unknown>)[String(valueKey)])
              const itemLabel = String((item as Record<string, unknown>)[String(labelKey)])
              return (
                <CommandItem
                  key={itemValue}
                  value={itemValue}
                  onSelect={() => {
                    // currentValue here is lowercase label from cmdk usually, 
                    // but we want the real ID. 
                    // However, we passed itemValue as value prop.
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
