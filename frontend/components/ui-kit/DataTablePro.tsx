import * as React from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { EmptyState } from "./EmptyState"
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  flexRender,
  ColumnDef,
  SortingState,
  ColumnFiltersState,
} from "@tanstack/react-table"
import { ArrowDownWideNarrow, ArrowUpWideNarrow, Search, ArrowUpDown } from "lucide-react"

type Column<T> = {
  key: keyof T | string
  header: React.ReactNode
  cell?: (row: T) => React.ReactNode
  className?: string
  sortable?: boolean
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
        if (!c.sortable) return c.header

        return (
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
        )
      },
      cell: ({ row }) =>
        c.cell
          ? c.cell(row.original)
          : (row.original as Record<string, unknown>)[c.key as string],
      meta: { className: c.className },
      enableSorting: c.sortable ?? false,
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
                {hg.headers.map((header) => {
                  const isSorted = header.column.getIsSorted()
                  return (
                    <th
                      key={header.id}
                      className="p-3 font-medium select-none"
                      onClick={header.column.getToggleSortingHandler()}
                      aria-sort={
                        isSorted === "asc" ? "ascending" : isSorted === "desc" ? "descending" : "none"
                      }
                    >
                      <div className="inline-flex items-center gap-1">
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {isSorted ? (
                          isSorted === "asc" ? (
                            <ArrowUpWideNarrow className="h-3.5 w-3.5 text-muted-foreground" />
                          ) : (
                            <ArrowDownWideNarrow className="h-3.5 w-3.5 text-muted-foreground" />
                          )
                        ) : null}
                      </div>
                    </th>
                  )
                })}
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
                    <td key={cell.id} className="p-3">
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
