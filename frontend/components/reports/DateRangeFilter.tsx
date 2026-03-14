"use client"

type DateRangeFilterProps = {
  fromDate: string
  toDate: string
  onChange: (next: { fromDate: string; toDate: string }) => void
  className?: string
}

export function DateRangeFilter({ fromDate, toDate, onChange, className }: DateRangeFilterProps) {
  return (
    <div className={className}>
      <div className="flex flex-wrap items-center gap-2">
        <input
          className="w-full max-w-[170px] px-3 py-2 rounded-md border border-input bg-background"
          type="date"
          value={fromDate}
          onChange={(e) => onChange({ fromDate: e.target.value, toDate })}
        />
        <input
          className="w-full max-w-[170px] px-3 py-2 rounded-md border border-input bg-background"
          type="date"
          value={toDate}
          onChange={(e) => onChange({ fromDate, toDate: e.target.value })}
        />
      </div>
    </div>
  )
}

