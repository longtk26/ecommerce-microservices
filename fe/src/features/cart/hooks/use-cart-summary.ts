"use client";

import { useCartStore } from "@/store/cart-store";
import type { TCartItem } from "@/types/cart";

type TCartSummaryReturn = {
  items: TCartItem[];
  shopId: string | null;
  shopName: string | null;
  itemCount: number;
  subtotal: number;
  total: number;
  isEmpty: boolean;
  removeItem: (id: string) => void;
  updateQuantity: (id: string, qty: number) => void;
  clearCart: () => void;
  closeCart: () => void;
};

export function useCartSummary(): TCartSummaryReturn {
  const items = useCartStore((state) => state.items);
  const shopId = useCartStore((state) => state.shopId);
  const shopName = useCartStore((state) => state.shopName);
  const removeItem = useCartStore((state) => state.removeItem);
  const updateQuantity = useCartStore((state) => state.updateQuantity);
  const clearCart = useCartStore((state) => state.clearCart);
  const closeCart = useCartStore((state) => state.closeCart);

  const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);
  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const total = subtotal; // No hidden fees or taxes in current backend specification

  return {
    items,
    shopId,
    shopName,
    itemCount,
    subtotal,
    total,
    isEmpty: items.length === 0,
    removeItem,
    updateQuantity,
    clearCart,
    closeCart,
  };
}
