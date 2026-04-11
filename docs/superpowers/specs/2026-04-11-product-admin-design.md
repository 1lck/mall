# Product Admin Frontend Design

**Date:** 2026-04-11

**Goal**

Build the first admin-facing frontend surface for product management inside this repository, using React, Vite, and Ant Design, while keeping product field definitions flexible until the backend CRUD contract is finalized.

**Scope**

- Add a standalone frontend app under `frontend/`
- Provide an admin shell with sidebar navigation and a content area
- Add product list, create, and edit flows
- Keep request and type definitions isolated so backend fields can be swapped in later
- Use temporary local mock data until real APIs are ready

**Out of Scope**

- Login and permission management
- Order, inventory, or customer-facing pages
- Final visual polish
- Real file upload

**Architecture**

The frontend will live as an independent Vite application inside the existing backend repository. Product UI will be organized by feature under `src/features/products`, while shared layout, routing, and app providers stay under `src/app`, `src/layouts`, and `src/components`.

To reduce rework, the product module will use a contract layer. Page components will render generic list and form structures, while product types, column definitions, form field metadata, and API adapters stay centralized. When backend entities and endpoints are ready, we only need to update the contract layer and service mapping instead of rewriting page flow.

**User Experience**

- Left sidebar with a clear "Product Management" navigation item
- Product list page with page title, action bar, create button, table, and row actions
- Create and edit page sharing one reusable form container
- Empty-state friendly experience while backend is not wired

**Data Strategy**

The product domain model will start as a lightweight placeholder structure with stable keys for identity, display title, status, and extensible metadata. Mock implementations will return deterministic in-memory data through the same service interface used by future real API calls.

**Routing**

- `/` redirects to `/products`
- `/products` shows the list
- `/products/new` shows create flow
- `/products/:productId/edit` shows edit flow

**Testing**

- Render-level tests for app shell navigation and default product page
- Product page behavior tests for loading mocked products and navigating to create flow
- Shared form tests for create/edit modes only where useful

**Backend Handoff**

The frontend will expect a replaceable service boundary:

- `listProducts()`
- `getProductById(productId)`
- `createProduct(input)`
- `updateProduct(productId, input)`
- `deleteProduct(productId)`

This boundary is intentionally not tied to final backend URLs yet.
