import { create } from "zustand";
import { devtools, persist } from "zustand/middleware";
import type { TCartState, TCartItem } from "@/types/cart";
import type { TProduct } from "@/types/product";

export const useCartStore = create<TCartState>()(
  devtools(
    persist(
      (set, get) => ({
        shopId: null,
        shopName: null,
        items: [],
        isOpen: false,
        pendingShopChange: null,

        addItem: (shopId: string, shopName: string, product: TProduct, quantity: number = 1) => {
          const currentShopId = get().shopId;
          const currentItems = get().items;

          const itemToAdd: TCartItem = {
            productId: product.id,
            productName: product.name,
            price: Number(product.price),
            imageUrl: product.imageUrl,
            quantity: quantity,
            stockQuantity: product.stockQuantity,
          };

          // If cart has items from a different shop, trigger pending confirmation
          if (currentShopId && currentShopId !== shopId && currentItems.length > 0) {
            set(
              {
                pendingShopChange: {
                  shopId,
                  shopName,
                  itemToAdd,
                },
              },
              false,
              "cart/triggerShopMismatch"
            );
            return false;
          }

          // Otherwise add/merge item into cart
          set(
            (state) => {
              const existingIndex = state.items.findIndex(
                (item) => item.productId === product.id
              );

              let newItems: TCartItem[];
              if (existingIndex > -1) {
                newItems = state.items.map((item, idx) => {
                  if (idx === existingIndex) {
                    const newQty = Math.min(
                      item.quantity + quantity,
                      product.stockQuantity
                    );
                    return { ...item, quantity: newQty };
                  }
                  return item;
                });
              } else {
                newItems = [...state.items, itemToAdd];
              }

              return {
                shopId,
                shopName,
                items: newItems,
                isOpen: true,
              };
            },
            false,
            "cart/addItem"
          );

          return true;
        },

        removeItem: (productId: string) => {
          set(
            (state) => {
              const newItems = state.items.filter(
                (item) => item.productId !== productId
              );
              return {
                items: newItems,
                shopId: newItems.length === 0 ? null : state.shopId,
                shopName: newItems.length === 0 ? null : state.shopName,
              };
            },
            false,
            "cart/removeItem"
          );
        },

        updateQuantity: (productId: string, quantity: number) => {
          if (quantity <= 0) {
            get().removeItem(productId);
            return;
          }

          set(
            (state) => ({
              items: state.items.map((item) => {
                if (item.productId === productId) {
                  const safeQty = Math.min(quantity, item.stockQuantity);
                  return { ...item, quantity: safeQty };
                }
                return item;
              }),
            }),
            false,
            "cart/updateQuantity"
          );
        },

        clearCart: () => {
          set(
            {
              shopId: null,
              shopName: null,
              items: [],
              pendingShopChange: null,
            },
            false,
            "cart/clearCart"
          );
        },

        confirmShopChange: () => {
          const pending = get().pendingShopChange;
          if (!pending) return;

          set(
            {
              shopId: pending.shopId,
              shopName: pending.shopName,
              items: [pending.itemToAdd],
              pendingShopChange: null,
              isOpen: true,
            },
            false,
            "cart/confirmShopChange"
          );
        },

        cancelShopChange: () => {
          set(
            { pendingShopChange: null },
            false,
            "cart/cancelShopChange"
          );
        },

        openCart: () => set({ isOpen: true }, false, "cart/openCart"),
        closeCart: () => set({ isOpen: false }, false, "cart/closeCart"),
        toggleCart: () =>
          set((state) => ({ isOpen: !state.isOpen }), false, "cart/toggleCart"),
      }),
      {
        name: "cart-storage",
        partialize: (state) => ({
          shopId: state.shopId,
          shopName: state.shopName,
          items: state.items,
        }),
      }
    )
  )
);
