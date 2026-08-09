# Epic 7 — Frontend (Buyer UI)

## Overview

The frontend is the buyer's window into the system. It demonstrates the saga from a user perspective — browse products, place an order, watch the status resolve in real-time.

**Tech Stack**: React Router v7 (framework mode) + Vite + Vanilla CSS  
**Key UX Challenge**: Orders are async. The buyer gets `PENDING` immediately — the frontend must poll until `COMPLETED` or `CANCELLED` and show a satisfying resolution screen.

---

## 🛠️ React Router v7 — Framework Mode Notes

React Router v7 in **framework mode** (formerly Remix) gives you:
- File-based routing (`app/routes/`)
- Server-side loaders (`loader` function) for data fetching
- Actions for form submissions
- No need for `useEffect` data fetching on most pages

```bash
# Create the project
npx create-react-router@latest frontend
cd frontend
npm install
npm run dev   # starts on http://localhost:5173
```

**Project structure:**
```
frontend/
├── app/
│   ├── root.tsx             ← global layout + styles
│   ├── routes/
│   │   ├── _index.tsx       ← / (home: shop list)
│   │   ├── shops.$shopId.tsx ← /shops/:shopId (product listing)
│   │   ├── checkout.tsx     ← /checkout
│   │   └── orders.$orderId.tsx ← /orders/:orderId (status polling)
│   └── styles/
│       └── global.css
├── public/
├── react-router.config.ts
└── package.json
```

---

## 🖼️ Screen Flow

```
┌───────────────────────────────────────────────┐
│  1. HOME  /                                   │
│     Shop cards: TechNest | FreshWear          │
│     loader: GET /api/shops (Inventory Svc)    │
└──────────────────────┬────────────────────────┘
                       │ click shop
                       ▼
┌───────────────────────────────────────────────┐
│  2. PRODUCT LISTING  /shops/:shopId           │
│     Product grid + stock badges               │
│     "Add to Cart" → cart context              │
│     loader: GET /api/shops/:id/products       │
└──────────────────────┬────────────────────────┘
                       │ click Checkout
                       ▼
┌───────────────────────────────────────────────┐
│  3. CHECKOUT  /checkout                       │
│     Cart summary + name field                 │
│     action: POST /api/orders (Order Svc)      │
│     → redirect to /orders/:orderId            │
└──────────────────────┬────────────────────────┘
                       │ redirect
                       ▼
┌───────────────────────────────────────────────┐
│  4. ORDER STATUS  /orders/:orderId            │
│     Polls GET /api/orders/:id every 2s        │
│     PENDING  → animated spinner               │
│     COMPLETED → 🎉 success screen             │
│     CANCELLED → ❌ failure screen             │
└───────────────────────────────────────────────┘
```

---

## 🎨 Design System

```css
/* app/styles/global.css */
:root {
  --bg:          #0f0f13;
  --surface:     #1a1a24;
  --surface-2:   #252535;
  --primary:     #6c63ff;
  --primary-glow:#6c63ff44;
  --success:     #22c55e;
  --error:       #ef4444;
  --warning:     #f59e0b;
  --text:        #f1f5f9;
  --text-muted:  #64748b;
  --border:      #2a2a3d;
  --radius:      12px;
  --radius-lg:   20px;
  --shadow:      0 4px 24px rgba(0,0,0,0.4);
}
```

**Typography**: Inter from Google Fonts  
**Key Animations:**
- Product cards: `translateY(-4px)` + glow shadow on hover
- Add to Cart: pulse on button click
- Order status: spinning orbit loader while PENDING
- COMPLETED: brief scale-up entrance animation
- CANCELLED: shake animation on error card

---

## 📋 Stories

### Story 7.1 — Project Setup + Design System
**As a developer**, I want a React Router v7 project with the design system ready.

**Acceptance Criteria:**
- [ ] Project created with `npx create-react-router@latest frontend` (framework mode)
- [ ] `app/styles/global.css` has all CSS variables, reset, and base typography
- [ ] Inter font loaded in `app/root.tsx` via Google Fonts link
- [ ] `.env` file has `VITE_ORDER_API_URL=http://localhost:8081` and `VITE_INVENTORY_API_URL=http://localhost:8082`
- [ ] CORS configured on backend services to allow `http://localhost:5173`
- [ ] `npm run dev` starts without errors

**CORS on Spring Boot:**
```java
// In each service's main controller or a global @Configuration
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:5173")
                    .allowedMethods("GET", "POST", "PUT", "DELETE");
        }
    };
}
```

---

### Story 7.2 — Home / Shop List Page (`/`)
**As a buyer**, I want to see all shops so I can choose where to shop.

**Acceptance Criteria:**
- [ ] `loader` function fetches `GET ${VITE_INVENTORY_API_URL}/api/shops`
- [ ] Renders shop cards: name, description, product count badge
- [ ] Cards have hover animation (glow + lift)
- [ ] Loading skeleton shown while loader runs
- [ ] Mobile responsive (1 column on small, 2 on desktop)

```tsx
// app/routes/_index.tsx
import type { Route } from "./+types/_index";

export async function loader() {
  const res = await fetch(`${process.env.INVENTORY_API_URL}/api/shops`);
  return await res.json();
}

export default function Home({ loaderData }: Route.ComponentProps) {
  return (
    <main className="home">
      <h1>🛒 ShopSaga</h1>
      <p className="subtitle">Distributed Commerce — Powered by Choreography</p>
      <div className="shop-grid">
        {loaderData.shops.map(shop => <ShopCard key={shop.id} shop={shop} />)}
      </div>
    </main>
  );
}
```

---

### Story 7.3 — Product Listing Page (`/shops/:shopId`)
**As a buyer**, I want to browse products and add them to my cart.

**Acceptance Criteria:**
- [ ] `loader` fetches `GET /api/shops/:shopId/products`
- [ ] Product grid: name, price, stock badge, image
- [ ] Stock badge: `🟢 In Stock (N)` → turns amber at ≤ 5, red/disabled at 0
- [ ] Out-of-stock: greyed card, "Out of Stock" badge, "Add" button disabled
- [ ] "Add to Cart" updates cart context (see Story 7.4)
- [ ] Cart icon in header shows item count badge

---

### Story 7.4 — Cart State (Context + localStorage)
**As a buyer**, I want my cart to persist across page navigation.

**Acceptance Criteria:**
- [ ] `CartContext` in `app/context/CartContext.tsx` provides: `items`, `addItem`, `removeItem`, `updateQty`, `clearCart`, `total`
- [ ] Cart saved to `localStorage` on every change, rehydrated on load
- [ ] Cart is scoped to one shop (adding from a different shop shows a confirmation dialog)
- [ ] Cart drawer slides in from right on cart icon click
- [ ] Drawer shows: item name, qty stepper, unit price, line total, cart total
- [ ] "Go to Checkout" button at bottom of drawer

---

### Story 7.5 — Checkout Page (`/checkout`)
**As a buyer**, I want to review my cart and place my order.

**Acceptance Criteria:**
- [ ] Order summary table: item name, qty, unit price, subtotal per item, grand total
- [ ] Single input: `name` (used to generate `userId`, e.g., `"John"` → `"user-john"`)
- [ ] "Place Order" button calls `POST ${VITE_ORDER_API_URL}/api/orders` with cart data
- [ ] Button shows loading spinner while awaiting response
- [ ] On `201` response: clear cart, navigate to `/orders/:orderId`
- [ ] On error: show inline error message (don't clear cart)

**Action (React Router v7):**
```tsx
// app/routes/checkout.tsx
export async function action({ request }: Route.ActionArgs) {
  const formData = await request.formData();
  const cartJson = formData.get("cart") as string;
  const name = formData.get("name") as string;
  const cart = JSON.parse(cartJson);

  const response = await fetch(`${process.env.ORDER_API_URL}/api/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId: `user-${name.toLowerCase().replace(/\s+/g, "-")}`,
      shopId: cart.shopId,
      items: cart.items.map((i: CartItem) => ({
        productId: i.productId,
        quantity: i.quantity,
      })),
    }),
  });

  if (!response.ok) throw new Error("Order failed");

  const { orderId } = await response.json();
  return redirect(`/orders/${orderId}`);
}
```

---

### Story 7.6 — Order Status Page (`/orders/:orderId`)
**As a buyer**, I want to see my order status resolve in real-time.

**Acceptance Criteria:**
- [ ] Initial load fetches `GET /api/orders/:orderId`
- [ ] While `status === "PENDING"`: animated spinner + "Processing your order..." text
- [ ] Polls every **2 seconds** (use `setInterval` in `useEffect`, stop on unmount)
- [ ] Max 30 poll attempts before showing "Taking longer than expected..." message
- [ ] On `COMPLETED`: success card with ✅ icon, order details, "Continue Shopping" button
- [ ] On `CANCELLED`: error card with ❌ icon, reason, "Try Again" button
- [ ] Page title updates dynamically: `"Processing..." → "Order Complete!" → "Order Cancelled"`

**Polling hook:**
```tsx
// app/hooks/useOrderStatus.ts
export function useOrderStatus(orderId: string, initialStatus: string) {
  const [order, setOrder] = useState<Order | null>(null);
  const [attempts, setAttempts] = useState(0);
  const MAX_ATTEMPTS = 30;

  useEffect(() => {
    if (!order || order.status === "PENDING" && attempts < MAX_ATTEMPTS) {
      const timer = setTimeout(async () => {
        const res = await fetch(`${import.meta.env.VITE_ORDER_API_URL}/api/orders/${orderId}`);
        const data = await res.json();
        setOrder(data);
        setAttempts(a => a + 1);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [order, attempts, orderId]);

  return { order, isTimeout: attempts >= MAX_ATTEMPTS };
}
```

---

## ✅ Epic 7 Definition of Done

- [ ] Home page shows both shops with cards
- [ ] Product listing shows all products with stock badges
- [ ] Out-of-stock products are visually disabled
- [ ] Cart persists in localStorage across navigation
- [ ] Checkout submits to Order Service and redirects to status page
- [ ] Order status page polls and shows COMPLETED/CANCELLED with distinct UI
- [ ] CORS configured — frontend can call both backend services
- [ ] Responsive on mobile viewport
