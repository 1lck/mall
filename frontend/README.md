# Mall Admin Frontend

React + Vite admin frontend for the mall practice project.

## Current Scope

- Admin shell with sidebar navigation
- Order management backed by live backend APIs
- Product management backed by live backend APIs
- Product and order create/edit pages sharing feature-level form components

## Commands

```bash
npm install
npm run dev
npm run test
npm run build
```

## Default Routes

- `/shop`
- `/shop/profile`
- `/orders`
- `/orders/new`
- `/orders/:orderId/edit`
- `/products`
- `/products/new`
- `/products/:productId/edit`

## Backend Integration Notes

- The storefront homepage at `/shop` reuses the live product API and only displays `ON_SALE` products.
- The order module uses the real backend endpoints under `http://localhost:8080/api/v1/orders` by default.
- The product module uses the real backend endpoints under `http://localhost:8080/api/v1/products` by default.
- You can override the backend base URL with `VITE_API_BASE_URL`.
- Both modules unwrap the backend `ApiResponse<T>` envelope in `src/shared/api/http.ts`.
