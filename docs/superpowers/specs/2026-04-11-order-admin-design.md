# Order Admin Frontend Design

**Date:** 2026-04-11

**Goal**

Add a Chinese admin-facing order management module that uses the live backend order APIs for list, detail, create, update, and delete flows.

**Scope**

- Add "订单管理" to the existing admin sidebar
- Add order list page, create page, edit page, and detail drawer
- Connect directly to `http://localhost:8080/api/v1/orders` through a replaceable API base URL
- Follow backend validation rules in the frontend forms

**API Contract**

- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `POST /api/v1/orders`
- `PUT /api/v1/orders/{id}`
- `DELETE /api/v1/orders/{id}`

All endpoints return the backend `ApiResponse<T>` wrapper. The frontend will unwrap `data` and surface backend `message` on failure.

**User Experience**

- Order list page shows order fields and row operations
- "查看" opens a right-side detail drawer that fetches the latest order detail
- "新建订单" and "编辑订单" use dedicated pages with shared form logic
- "删除" uses a confirmation pop-up and refreshes the list on success

**Validation**

- Create: `userId` required, `totalAmount` required and `> 0`, `remark <= 255`
- Update: `totalAmount` required and `> 0`, `status` required, `remark <= 255`

**Out of Scope**

- Order search and filter
- Pagination
- Batch actions
- Product-order linking UI
