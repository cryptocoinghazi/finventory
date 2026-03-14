import * as React from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { EmptyState } from "./EmptyState"
import { cn } from "@/lib/utils"
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  flexRender,
  ColumnDef,
  SortingState,
  ColumnFiltersState,
  SortingFn,
} from "@tanstack/react-table"
import { Search, ArrowUpDown, ArrowDownWideNarrow, ArrowUpWideNarrow } from "lucide-react"

type Column<T> = {
  key: keyof T | string
  header: React.ReactNode
  cell?: (row: T) => React.ReactNode
  className?: string
  sortable?: boolean
  filterable?: boolean
  filterPlaceholder?: string
}

const caseInsensitiveAutoSort: SortingFn<unknown> = (rowA, rowB, columnId) => {
  const a = rowA.getValue(columnId)
  const b = rowB.getValue(columnId)

  if (a == null && b == null) return 0
  if (a == null) return -1
  if (b == null) return 1

  if (typeof a === "number" && typeof b === "number") return a - b
  if (a instanceof Date && b instanceof Date) return a.getTime() - b.getTime()

  const aNum = typeof a === "string" ? Number(a) : NaN
  const bNum = typeof b === "string" ? Number(b) : NaN
  const bothNumeric =
    typeof a !== "boolean" &&
    typeof b !== "boolean" &&
    Number.isFinite(aNum) &&
    Number.isFinite(bNum)
  if (bothNumeric) return aNum - bNum

  const aStr = String(a).toLowerCase()
  const bStr = String(b).toLowerCase()
  return aStr.localeCompare(bStr)
}

export function DataTablePro<T>({
  columns,
  data,
  loading,
  filters,
  actions,
  searchKey,
  empty,
  page,
  pageSize,
  total,
  onPageChange,
  stickyHeader,
}: {
  columns: Column<T>[]
  data: T[]
  loading?: boolean
  filters?: React.ReactNode
  actions?: React.ReactNode
  searchKey?: string
  empty?: { title: React.ReactNode; description?: React.ReactNode; onAdd?: () => void }
  page?: number
  pageSize?: number
  total?: number
  onPageChange?: (nextPage: number) => void
  stickyHeader?: boolean
}) {
  const totalPages =
    page !== undefined && pageSize && total !== undefined
      ? Math.max(1, Math.ceil(total / pageSize))
      : undefined

  const [sorting, setSorting] = React.useState<SortingState>([])
  const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>([])

  const columnDefs = React.useMemo<ColumnDef<T>[]>(() => {
    return columns.map((c) => ({
      id: String(c.key),
      accessorKey: typeof c.key === "string" ? c.key : String(c.key),
      header: ({ column }) => {
        const headerLabel = c.sortable ? (
          <Button
            variant="ghost"
            onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
            className="-ml-4 h-8 data-[state=open]:bg-accent"
          >
            {c.header}
            {column.getIsSorted() === "desc" ? (
              <ArrowDownWideNarrow className="ml-2 h-4 w-4" />
            ) : column.getIsSorted() === "asc" ? (
              <ArrowUpWideNarrow className="ml-2 h-4 w-4" />
            ) : (
              <ArrowUpDown className="ml-2 h-4 w-4" />
            )}
          </Button>
        ) : (
          c.header
        )

        if (!c.filterable) return headerLabel

        return (
          <div className="flex flex-col gap-2">
            <div>{headerLabel}</div>
            <Input
              placeholder={c.filterPlaceholder ?? "Filter..."}
              value={(column.getFilterValue() as string) ?? ""}
              onChange={(event) => column.setFilterValue(event.target.value)}
              onClick={(e) => e.stopPropagation()}
              onKeyDown={(e) => e.stopPropagation()}
              className="h-8"
            />
          </div>
        )
      },
      cell: ({ row }) =>
        c.cell
          ? c.cell(row.original)
          : (row.original as Record<string, unknown>)[c.key as string],
      meta: { className: c.className },
      enableSorting: c.sortable ?? false,
      sortingFn: c.sortable ? (caseInsensitiveAutoSort as SortingFn<T>) : undefined,
    }))
  }, [columns])

  const table = useReactTable({
    data,
    columns: columnDefs,
    state: { sorting, columnFilters },
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
  })

  return (
    <div className="space-y-4">
      {filters || actions || searchKey ? (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-1 items-center gap-2">
            {searchKey && (
              <div className="relative max-w-sm flex-1">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search..."
                  value={(table.getColumn(searchKey)?.getFilterValue() as string) ?? ""}
                  onChange={(event) =>
                    table.getColumn(searchKey)?.setFilterValue(event.target.value)
                  }
                  className="pl-9"
                />
              </div>
            )}
            {filters ? <div className="flex-1">{filters}</div> : null}
          </div>
          {actions ? <div className="flex items-center gap-2">{actions}</div> : null}
        </div>
      ) : null}

      <div className="rounded-2xl border border-border overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-muted/50">
            {table.getHeaderGroups().map((hg) => (
              <tr key={hg.id} className="text-left">
                {hg.headers.map((header) => (
                  <th
                    key={header.id}
                    className={cn(
                      "p-3 font-medium select-none",
                      stickyHeader ? "sticky top-0 z-10 bg-muted/50" : undefined,
                      (header.column.columnDef.meta as { className?: string } | undefined)
                        ?.className
                    )}
                  >
                    {flexRender(header.column.columnDef.header, header.getContext())}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody>
            {loading ? (
              [...Array(6)].map((_, i) => (
                <tr key={i} className="border-t border-border">
                  {columns.map((_, j) => (
                    <td key={j} className="p-3">
                      <div className="h-4 w-full max-w-[180px] bg-muted animate-pulse rounded" />
                    </td>
                  ))}
                </tr>
              ))
            ) : data.length === 0 ? (
              <tr>
                <td className="p-4" colSpan={columns.length}>
                  {empty ? (
                    <EmptyState
                      title={empty.title}
                      description={empty.description}
                      action={
                        empty.onAdd
                          ? { label: "Add", onClick: empty.onAdd }
                          : undefined
                      }
                    />
                  ) : (
                    <div className="text-sm text-muted-foreground">No data</div>
                  )}
                </td>
              </tr>
            ) : (
              table.getRowModel().rows.map((row) => (
                <tr key={row.id} className="border-t border-border">
                  {row.getVisibleCells().map((cell) => (
                    <td
                      key={cell.id}
                      className={cn(
                        "p-3",
                        (cell.column.columnDef.meta as { className?: string } | undefined)
                          ?.className
                      )}
                    >
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages ? (
        <div className="flex items-center justify-end gap-2">
          <Button
            variant="outline"
            disabled={!onPageChange || page === 1}
            onClick={() => onPageChange && page && onPageChange(page - 1)}
          >
            Previous
          </Button>
          <div className="text-sm text-muted-foreground">
            Page {page} of {totalPages}
          </div>
          <Button
            variant="outline"
            disabled={!onPageChange || page === totalPages}
            onClick={() => onPageChange && page && onPageChange(page + 1)}
          >
            Next
          </Button>
        </div>
      ) : null}
    </div>
  )
}
