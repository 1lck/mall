# Shop Home Frontend Design

**Date:** 2026-04-11

**Goal**

Add a customer-facing storefront homepage inside the existing frontend app, with a marketplace-style browsing experience, dynamic category tabs, product cards, and a personal center entry.

**Scope**

- Add a storefront route group under `/shop`
- Add a customer homepage at `/shop`
- Add a profile placeholder page at `/shop/profile`
- Reuse the live product API and only display `ON_SALE` products
- Build category tabs from product `categoryName`

**User Experience**

- Clean, white, marketplace-like layout inspired by mainstream shopping homepages
- Top navigation with brand mark, lightweight actions, and "个人中心" on the right
- Horizontal category tabs with "精选" as the default
- Responsive product grid with strong image placeholder area, product name, category label, and price

**Data Strategy**

- Use `GET /api/v1/products`
- Filter products to `status === ON_SALE`
- Generate category tabs dynamically from the returned `categoryName`
- Use a visual placeholder for product images because the current API does not expose image URLs

**Out of Scope**

- Login flow
- Cart
- Product detail page
- Search backend
- Recommendation algorithms
