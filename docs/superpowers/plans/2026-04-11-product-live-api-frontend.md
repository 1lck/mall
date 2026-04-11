# Product Live API Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Switch the existing product admin frontend from local mock data to the live backend product APIs while matching the final backend field contract and validation rules.

**Architecture:** Reuse the existing product routes and shared page structure, but replace placeholder types and service logic with a real HTTP-backed contract. Keep UI behavior stable, centralize API access in the product service layer, and surface backend messages when requests fail.

**Tech Stack:** React, TypeScript, Vite, Ant Design, React Router, Vitest, Testing Library

---

### Task 1: Add failing product live-API coverage

**Files:**
- Modify: `frontend/src/features/products/pages/ProductListPage.test.tsx`
- Modify: `frontend/src/features/products/components/ProductForm.test.tsx`

- [ ] Replace the old mock-based expectations with tests that mock backend product responses and assert the real product fields render
- [ ] Run `npm run test -- src/features/products/pages/ProductListPage.test.tsx`
- [ ] Run `npm run test -- src/features/products/components/ProductForm.test.tsx`

### Task 2: Replace product types and service layer

**Files:**
- Modify: `frontend/src/features/products/types.ts`
- Modify: `frontend/src/features/products/service.ts`
- Delete usage from: `frontend/src/features/products/mockProducts.ts`

- [ ] Replace placeholder product types with backend-aligned types
- [ ] Replace the local mock product service with real HTTP requests through the shared request helper
- [ ] Re-run the focused product tests and confirm the service-backed UI still passes

### Task 3: Update product list and form UI

**Files:**
- Modify: `frontend/src/features/products/pages/ProductListPage.tsx`
- Modify: `frontend/src/features/products/components/ProductForm.tsx`
- Modify: `frontend/src/features/products/pages/ProductCreatePage.tsx`
- Modify: `frontend/src/features/products/pages/ProductEditPage.tsx`

- [ ] Update the list table to show `productNo`, `categoryName`, and real status labels
- [ ] Update the shared form so create/edit match backend field and validation rules
- [ ] Keep create/edit success and error handling consistent with the new order module

### Task 4: Verify and document

**Files:**
- Modify: `frontend/README.md`

- [ ] Run `npm run test`
- [ ] Run `npm run lint`
- [ ] Run `npm run build`
- [ ] Update README wording to mention that product APIs now use the live backend endpoints
