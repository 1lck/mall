# Product Admin Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a React + Vite + Ant Design admin frontend skeleton for product CRUD with mock-backed list/create/edit flows and a replaceable contract layer for future backend integration.

**Architecture:** Add a standalone `frontend/` app inside the repository. Keep routing, layout, shared providers, and product feature code separated so the backend contract can change with minimal page churn. Start with mock services but preserve a service interface that can later wrap real HTTP calls.

**Tech Stack:** React, TypeScript, Vite, Ant Design, React Router, Vitest, Testing Library

---

### Task 1: Scaffold Frontend Workspace

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/src/*`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig*.json`
- Create: `frontend/index.html`

- [ ] Generate a Vite React TypeScript app in `frontend/`
- [ ] Install runtime dependencies: `antd`, `react-router-dom`
- [ ] Install test dependencies and configure Vitest with jsdom and Testing Library
- [ ] Verify the generated app starts and tests can run

### Task 2: Add Failing App Shell Test

**Files:**
- Create: `frontend/src/app/App.test.tsx`
- Create: `frontend/src/test/setup.ts`

- [ ] Write a failing test asserting that the admin shell renders a sidebar label and lands on the product list page
- [ ] Run the focused Vitest command and confirm the test fails for the expected missing UI

### Task 3: Implement App Shell and Routing

**Files:**
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/app/App.tsx`
- Create: `frontend/src/app/router.tsx`
- Create: `frontend/src/layouts/AdminLayout.tsx`
- Create: `frontend/src/styles/global.css`

- [ ] Implement the Ant Design application shell with a sidebar and routed content area
- [ ] Add routes for product list, create, and edit pages
- [ ] Re-run the focused test and confirm it passes

### Task 4: Add Failing Product List Test

**Files:**
- Create: `frontend/src/features/products/pages/ProductListPage.test.tsx`

- [ ] Write a failing test asserting mocked product rows render and the create button is visible
- [ ] Run the focused test and confirm it fails for missing product feature code

### Task 5: Implement Product Contract and List Flow

**Files:**
- Create: `frontend/src/features/products/types.ts`
- Create: `frontend/src/features/products/mockProducts.ts`
- Create: `frontend/src/features/products/service.ts`
- Create: `frontend/src/features/products/pages/ProductListPage.tsx`

- [ ] Implement flexible product types and mock product data
- [ ] Implement the service layer with async list/get/create/update/delete functions
- [ ] Implement the list page with action bar, table, status display, and row actions
- [ ] Re-run the focused list test and confirm it passes

### Task 6: Add Failing Product Form Test

**Files:**
- Create: `frontend/src/features/products/components/ProductForm.test.tsx`

- [ ] Write a failing test asserting create mode and edit mode headings render through the shared form page
- [ ] Run the focused test and confirm it fails for the missing form flow

### Task 7: Implement Create/Edit Flow

**Files:**
- Create: `frontend/src/features/products/components/ProductForm.tsx`
- Create: `frontend/src/features/products/pages/ProductCreatePage.tsx`
- Create: `frontend/src/features/products/pages/ProductEditPage.tsx`

- [ ] Build a shared form component with placeholder-friendly fields
- [ ] Implement create and edit pages wired to the service layer and success messages
- [ ] Re-run the focused form test and confirm it passes

### Task 8: Regression Verification

**Files:**
- Modify: `frontend/README.md`

- [ ] Run the full frontend test suite
- [ ] Run the frontend production build
- [ ] Add a short frontend README section with install and run commands
