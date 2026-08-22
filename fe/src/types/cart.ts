import type { TProduct } from "./product";

export type TCartItem = {
  productId: string;
  productName: string;
  price: number;
  imageUrl?: string;
  quantity: number;
  stockQuantity: number;
};

export type TCartState = {
  shopId: string | null;
  shopName: string | null;
  items: TCartItem[];
  isOpen: boolean;
  pendingShopChange: { shopId: string; shopName: string; itemToAdd: TCartItem } | null;
  
  // Actions
  addItem: (shopId: string, shopName: string, product: TProduct, quantity?: number) => boolean;
  removeItem: (productId: string) => void;
  updateQuantity: (productId: string, quantity: number) => void;
  clearCart: () => void;
  confirmShopChange: () => void;
  cancelShopChange: () => void;
  openCart: () => void;
  closeCart: () => void;
  toggleCart: () => void;
};
