"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCartStore } from "@/store/cart-store";
import { Button } from "@/components/ui/button";
import { ShoppingBag, Store, Layers, Sparkles } from "lucide-react";
import { Container } from "@/components/layout/Container";

export function Header() {
  const pathname = usePathname();
  const items = useCartStore((state) => state.items);
  const openCart = useCartStore((state) => state.openCart);
  const shopName = useCartStore((state) => state.shopName);

  const totalItemsCount = items.reduce((acc, item) => acc + item.quantity, 0);

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/70 bg-background/80 backdrop-blur-xl transition-all">
      <Container size="xl">
        <div className="flex h-16 items-center justify-between gap-4">
          {/* Logo & Brand */}
          <div className="flex items-center gap-6">
            <Link
              href="/"
              className="flex items-center gap-2.5 group transition-transform active:scale-95"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-purple-500 shadow-md shadow-indigo-500/25 group-hover:shadow-indigo-500/50 transition-all">
                <Sparkles className="h-5 w-5 text-white" />
              </div>
              <div className="flex flex-col">
                <span className="text-base font-bold tracking-tight bg-gradient-to-r from-white via-slate-200 to-indigo-200 bg-clip-text text-transparent">
                  ShopSaga
                </span>
                <span className="text-[10px] uppercase font-semibold tracking-wider text-muted-foreground">
                  Event Choreography
                </span>
              </div>
            </Link>

            {/* Navigation links */}
            <nav className="hidden md:flex items-center gap-1">
              <Link
                href="/"
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  pathname === "/"
                    ? "bg-secondary text-foreground"
                    : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
                }`}
              >
                <Store className="h-3.5 w-3.5" />
                All Shops
              </Link>
            </nav>
          </div>

          {/* Right Action Bar */}
          <div className="flex items-center gap-3">
            {shopName && (
              <div className="hidden sm:flex items-center gap-1.5 rounded-full border border-indigo-500/20 bg-indigo-500/10 px-3 py-1 text-xs text-indigo-300">
                <Store className="h-3.5 w-3.5" />
                <span>Active: <strong>{shopName}</strong></span>
              </div>
            )}

            {/* Cart Button */}
            <Button
              onClick={openCart}
              variant="outline"
              size="default"
              className="relative gap-2 border-border/80 bg-card/80 hover:border-primary/50 shadow-sm"
              aria-label="Open Cart"
            >
              <ShoppingBag className="h-4 w-4 text-primary" />
              <span className="text-sm font-medium">Cart</span>
              {totalItemsCount > 0 && (
                <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[11px] font-bold text-primary-foreground animate-in zoom-in-50 duration-200">
                  {totalItemsCount}
                </span>
              )}
            </Button>
          </div>
        </div>
      </Container>
    </header>
  );
}
