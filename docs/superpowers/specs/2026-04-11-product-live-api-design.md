# Product Live API Frontend Design

**Date:** 2026-04-11

**Goal**

Replace the temporary mock-backed product admin module with the real backend product CRUD APIs while preserving the current list/create/edit page structure.

**Scope**

- Keep `/products`, `/products/new`, and `/products/:productId/edit`
- Replace local mock product service with live API calls
- Update product types, list columns, and form fields to match the backend contract
- Keep Chinese UI and backend-first error handling

**API Contract**

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `DELETE /api/v1/products/{id}`

**Backend Fields**

- `id`
- `productNo`
- `name`
- `categoryName`
- `price`
- `stock`
- `status`
- `description`
- `createdAt`
- `updatedAt`

**Validation**

- `name`: required, max 120
- `categoryName`: required, max 100
- `price`: required, `> 0`
- `stock`: required, `>= 0`
- `description`: optional, max 500

**Status Mapping**

- `DRAFT` -> `草稿`
- `ON_SALE` -> `在售`
- `OFF_SHELF` -> `已下架`
