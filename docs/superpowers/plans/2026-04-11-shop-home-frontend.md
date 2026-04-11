# Shop Home Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a storefront homepage and profile entry for the customer-facing side of the mall app using the existing live product API.

**Architecture:** Add a separate `/shop` route tree and layout so the storefront and admin console stay isolated while still sharing the same product service layer. Keep storefront logic focused on category derivation, ON_SALE filtering, and presentational product cards with image placeholders.

**Tech Stack:** React, TypeScript, Vite, Ant Design, React Router, Vitest, Testing Library

---

### Task 1: Add failing storefront route coverage

**Files:**
- Create: `frontend/src/features/shop/pages/ShopHomePage.test.tsx`
- Modify: `frontend/src/app/router.tsx`

- [ ] Write a failing test that expects `/shop` to render category tabs, ON_SALE product cards, and a "个人中心" entry
- [ ] Run `npm run test -- src/features/shop/pages/ShopHomePage.test.tsx` and confirm it fails because the storefront feature does not exist yet

### Task 2: Implement storefront layout and homepage

**Files:**
- Create: `frontend/src/layouts/ShopLayout.tsx`
- Create: `frontend/src/features/shop/pages/ShopHomePage.tsx`
- Create: `frontend/src/features/shop/pages/ProfilePage.tsx`
- Modify: `frontend/src/app/router.tsx`
- Modify: `frontend/src/styles/global.css`

- [ ] Add a storefront layout with top navigation and customer-facing spacing
- [ ] Implement the shop homepage using live products, ON_SALE filtering, and derived category tabs
- [ ] Re-run `npm run test -- src/features/shop/pages/ShopHomePage.test.tsx` and confirm it passes

### Task 3: Add profile route and polish

**Files:**
- Modify: `frontend/src/layouts/ShopLayout.tsx`
- Modify: `frontend/README.md`

- [ ] Add the profile placeholder page and route wiring
- [ ] Update README with storefront routes

### Task 4: Verify

**Files:**
- No additional files

- [ ] Run `npm run test`
- [ ] Run `npm run lint`
- [ ] Run `npm run build`
