# Order Admin Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a live order management module in the existing admin frontend, including list, detail drawer, create, edit, and delete flows backed by the real backend order APIs.

**Architecture:** Reuse the current admin shell and add a new `orders` feature module with dedicated types, service functions, and page/components. Centralize HTTP wrapper logic so backend response envelopes and error messages are handled in one place, while order pages focus on rendering and user interaction.

**Tech Stack:** React, TypeScript, Vite, Ant Design, React Router, Vitest, Testing Library

---

### Task 1: Add failing order list coverage

**Files:**
- Create: `frontend/src/features/orders/pages/OrderListPage.test.tsx`
- Modify: `frontend/src/layouts/AdminLayout.tsx`
- Modify: `frontend/src/app/router.tsx`

- [ ] Write a failing test that expects the order menu and order list page to render with a "新建订单" button and a fetched order row
- [ ] Run `npm run test -- src/features/orders/pages/OrderListPage.test.tsx` and confirm it fails because the order feature does not exist yet

### Task 2: Implement order service and list page

**Files:**
- Create: `frontend/src/shared/api/http.ts`
- Create: `frontend/src/features/orders/types.ts`
- Create: `frontend/src/features/orders/service.ts`
- Create: `frontend/src/features/orders/pages/OrderListPage.tsx`
- Create: `frontend/src/features/orders/components/OrderDetailDrawer.tsx`
- Modify: `frontend/src/app/router.tsx`
- Modify: `frontend/src/layouts/AdminLayout.tsx`

- [ ] Add a small HTTP helper that unwraps backend `ApiResponse<T>` and throws readable errors
- [ ] Add order types and service functions for list/get/create/update/delete
- [ ] Implement the order list page with table, actions, and detail drawer state
- [ ] Re-run `npm run test -- src/features/orders/pages/OrderListPage.test.tsx` and confirm it passes

### Task 3: Add failing order form coverage

**Files:**
- Create: `frontend/src/features/orders/components/OrderForm.test.tsx`

- [ ] Write a failing test for shared create/edit order form labels and prefilled edit values
- [ ] Run `npm run test -- src/features/orders/components/OrderForm.test.tsx` and confirm it fails because the form does not exist yet

### Task 4: Implement order create/edit flow

**Files:**
- Create: `frontend/src/features/orders/components/OrderForm.tsx`
- Create: `frontend/src/features/orders/pages/OrderCreatePage.tsx`
- Create: `frontend/src/features/orders/pages/OrderEditPage.tsx`
- Modify: `frontend/src/app/router.tsx`

- [ ] Implement the shared order form with frontend validation matching backend rules
- [ ] Implement create and edit pages backed by the live order service
- [ ] Re-run `npm run test -- src/features/orders/components/OrderForm.test.tsx` and confirm it passes

### Task 5: Verify and document

**Files:**
- Modify: `frontend/README.md`

- [ ] Run `npm run test`
- [ ] Run `npm run lint`
- [ ] Run `npm run build`
- [ ] Update the frontend README to mention the order module and API base URL override
