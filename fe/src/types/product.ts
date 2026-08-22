export type TProduct = {
  id: string;
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  stockQuantity: number;
  inStock: boolean;
};

export type TStockStatus = "in_stock" | "low_stock" | "out_of_stock";

export function getStockStatus(stockQuantity: number, inStock: boolean): TStockStatus {
  if (!inStock || stockQuantity <= 0) return "out_of_stock";
  if (stockQuantity <= 5) return "low_stock";
  return "in_stock";
}
