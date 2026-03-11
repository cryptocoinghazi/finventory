Build a complete **Label Printing Module** for Finventory, similar to NexoPOS label printing, focused on retail apparel use cases such as dresses, hijabs, and general items.

IMPORTANT:
- Build this as a proper Finventory module, not a demo
- Reuse existing Finventory backend + frontend architecture
- Keep the UI clean, fast, and easy for store staff
- Do not break existing item, POS, or barcode workflows
- The printed labels must be scannable in Quick POS using a standard barcode scanner

GOAL
Allow users to select one or multiple items from Finventory and print barcode sticker labels for attaching to dresses/items. These labels will later be scanned in Quick POS for checkout.

==================================================
1. CORE USER FLOW
==================================================

User flow should be:

1. User opens Items page
2. User selects one or more items
3. User clicks “Print Labels”
4. A label printing dialog/page opens
5. User can:
   - review selected items
   - enter sticker quantity for each item
   - choose barcode format if needed
   - choose label size/template
6. User previews the labels
7. User confirms print
8. Labels are printed through browser/system printer
9. Print activity is logged for audit

==================================================
2. MAIN REQUIREMENTS
==================================================

A) ITEM SELECTION
- Support printing labels for:
  - single item
  - multiple selected items
- Add bulk action from Items list:
  - “Print Labels”
- Also support printing from item detail page for a single item

B) LABEL DATA
Each label must include:
- Barcode
- Item name
- Selling price
Optional:
- SKU / item code
- short category/vendor text if space allows

C) BARCODE
- Use the item’s existing barcode
- Barcode must be scannable by standard POS barcode scanners
- Prefer Code 128 by default
- Support EAN-13 if item barcode matches EAN-13 rules
- Validate barcode before allowing print
- If barcode missing or invalid, show clear error and block printing for that item

D) QUANTITY PER ITEM
- User must enter label quantity per selected item
- Default quantity = 1
- Quantity must be positive integer
- Support fast editing for multiple rows
- Show total label count live

E) LABEL SIZE / TEMPLATE
Support configurable label templates
Default template:
- 2 inch x 1 inch

Also structure code so more templates can be added later.

Default label design:
- Barcode centered
- Item name clearly readable
- Selling price bold and visible
- clean thermal-printer friendly layout
- optimized for sticker sheets / thermal label printers

F) PRINT PREVIEW
Provide print preview before final printing:
- show exact labels as they will print
- support zoom in/out
- support pagination if many labels
- show template size
- print-friendly layout

G) PRINT ACTION
- Use browser/system print flow
- Open print-friendly label sheet/page or modal
- Ensure CSS print layout is optimized
- Labels must align consistently on desktop/tablet browsers

H) VALIDATION
Before printing:
- barcode must exist
- barcode must be valid for selected format
- selling price must exist
- quantity must be > 0

If some selected items are invalid:
- show list of invalid items
- allow user to remove invalid items from print batch

I) AUDIT LOGGING
Log every label print action with:
- timestamp
- logged-in user
- item ids
- item names
- barcode values
- quantity per item
- total labels
- template used
- success/failure status

==================================================
3. FRONTEND REQUIREMENTS
==================================================

Tech:
- Use existing Finventory frontend stack
- Reuse existing UI system/components
- Keep it responsive for desktop and tablet

Build these UI pieces:

1. Items table bulk action
- Add checkbox multi-select if not already present
- Add “Print Labels” bulk action button

2. Label Print Dialog / Drawer / Page
Must include:
- selected items table
- item image optional if already available
- editable quantity field per item
- barcode preview/validation indicator
- selling price preview
- total label count
- template selector
- barcode format selector (default Code 128, auto-use EAN-13 where valid if supported)

3. Print Preview section
- preview generated labels
- zoom control
- page count / total labels
- print button
- cancel button

4. Error handling UI
- inline validation errors
- banner for batch issues
- failed item rows highlighted

UX requirements:
- very easy for non-technical store staff
- minimal steps
- clean, compact, efficient workflow
- works well for printing many labels quickly

==================================================
4. BACKEND REQUIREMENTS
==================================================

Build backend support where needed.

Required backend features:

A) Label print request model
Accept:
- item ids
- quantity per item
- template
- barcode format

B) Validation service
Validate:
- item exists
- selling price exists
- barcode exists
- barcode format valid

C) Audit logging persistence
Create print audit entity/table if needed:
Suggested fields:
- id
- user_id
- printed_at
- total_labels
- template_name
- barcode_format
- status
- details_json

D) Optional preview payload endpoint
If useful, provide backend-prepared preview data for label rendering

IMPORTANT:
- Do not overcomplicate backend if frontend can safely render labels from existing item data
- But audit logging must be persisted

==================================================
5. LABEL RENDERING REQUIREMENTS
==================================================

Use a reliable barcode rendering library.

Requirements:
- barcode must render clearly in browser
- printable at small label size
- high contrast
- no blurry scaling
- thermal-printer friendly
- name and price should not overlap barcode

Print CSS must:
- remove unnecessary margins
- enforce correct label dimensions
- repeat labels according to quantity
- support multiple pages
- keep page breaks stable

==================================================
6. PERFORMANCE REQUIREMENTS
==================================================

- Must handle 50 labels smoothly
- Preview/render should feel responsive
- Printing preparation should complete quickly
- Avoid unnecessary API calls per label
- Generate labels in batch

==================================================
7. EDGE CASES
==================================================

Handle these cases:
- item without barcode
- invalid barcode format
- item without selling price
- zero quantity
- negative quantity
- duplicate selected items
- large batch print
- mixed valid/invalid selection
- user cancels preview
- print action opened but not completed

==================================================
8. TESTS
==================================================

Add tests for:
- single item label printing flow
- multiple item batch printing
- quantity validation
- barcode validation
- missing price handling
- preview generation
- total label count calculation
- audit log creation

If frontend tests exist:
- test label dialog interactions
- test invalid item blocking
- test preview rendering logic

If backend tests exist:
- test validation service
- test audit log persistence

==================================================
9. DELIVERABLES
==================================================

When done, provide:
1. files added/changed
2. backend changes made
3. frontend screens/components added
4. how label printing is triggered from Items
5. how preview works
6. how audit logging works
7. supported barcode formats
8. limitations if any

==================================================
10. IMPLEMENTATION STYLE
==================================================

- Keep code production-ready
- Keep module extensible for future templates
- Keep UI simple and professional
- Do not break Quick POS barcode scanning compatibility
- Prioritize real usability for retail staff