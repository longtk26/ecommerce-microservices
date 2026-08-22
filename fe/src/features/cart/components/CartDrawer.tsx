"use client";

import * as React from "react";
import Link from "next/link";
import { useCartStore } from "@/store/cart-store";
import { useCartSummary } from "../hooks/use-cart-summary";
import { CartItemRow } from "./CartItemRow";
import { CartSummary } from "./CartSummary";
import { Button } from "@/components/ui/button";
import {
  ShoppingBag,
  X,
  Trash2,
  ArrowRight,
  Store,
  Sparkles,
} from "lucide-react";

export function CartDrawer() {
  const isOpen = useCartStore((state) => state.isOpen);
  const {
    items,
    shopName,
    itemCount,
    subtotal,
    total,
    isEmpty,
    removeItem,
    updateQuantity,
    clearCart,
    closeCart,
  } = useCartSummary();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden animate-in fade-in duration-200">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/70 backdrop-blur-sm transition-opacity"
        onClick={closeCart}
      />

      {/* Drawer Panel */}
      <div className="fixed inset-y-0 right-0 flex max-w-full pl-10">
        <div className="w-screen max-w-md border-l border-border/80 bg-card/95 backdrop-blur-2xl shadow-2xl flex flex-col justify-between animate-in slide-in-from-right duration-300">
          {/* Header */}
          <div className="flex items-center justify-between border-b border-border/60 p-5">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500/20 text-indigo-400">
                <ShoppingBag className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-foreground">Shopping Cart</h2>
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  {shopName ? (
                    <>
                      <Store className="h-3 w-3 text-indigo-400" />
                      <span>{shopName}</span>
                    </>
                  ) : (
                    <span>{itemCount} items</span>
                  )}
                </div>
              </div>
            </div>

            <button
              type="button"
              onClick={closeCart}
              className="rounded-lg p-2 text-muted-foreground hover:bg-secondary hover:text-foreground transition-colors"
              aria-label="Close cart"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Cart Item List */}
          <div className="flex-1 overflow-y-auto p-5 space-y-3">
            {isEmpty ? (
              <div className="flex h-full flex-col items-center justify-center text-center space-y-4 py-12">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-secondary/80 text-muted-foreground">
                  <ShoppingBag className="h-8 w-8 stroke-1" />
                </div>
                <div className="space-y-1">
                  <h3 className="text-base font-semibold text-foreground">Your cart is empty</h3>
                  <p className="text-xs text-muted-foreground max-w-[240px]">
                    Browse our shops and add products to start your order.
                  </p>
                </div>
                <Button onClick={closeCart} variant="outline" size="sm" className="gap-2">
                  <Store className="h-4 w-4" />
                  Browse Shops
                </Button>
              </div>
            ) : (
              items.map((item) => (
                <CartItemRow
                  key={item.productId}
                  item={item}
                  onUpdateQuantity={updateQuantity}
                  onRemove={removeItem}
                />
              ))
            )}
          </div>

          {/* Footer Actions */}
          {!isEmpty && (
            <div className="border-t border-border/60 bg-secondary/20 p-5 space-y-4">
              <CartSummary itemCount={itemCount} subtotal={subtotal} total={total} />

              <div className="space-y-2">
                <Link href="/checkout" onClick={closeCart} className="w-full block">
                  <Button variant="glow" size="lg" className="w-full justify-between">
                    <span className="flex items-center gap-2">
                      <Sparkles className="h-4 w-4" />
                      Proceed to Checkout
                    </span>
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>

                <Button
                  onClick={clearCart}
                  variant="ghost"
                  size="sm"
                  className="w-full text-xs text-muted-foreground hover:text-destructive gap-1.5"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  Clear Cart
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
