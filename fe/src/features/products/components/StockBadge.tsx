import * as React from "react";
import { Badge } from "@/components/ui/badge";
import { getStockStatus } from "@/types/product";

type StockBadgeProps = {
  stockQuantity: number;
  inStock: boolean;
};

export function StockBadge({ stockQuantity, inStock }: StockBadgeProps) {
  const status = getStockStatus(stockQuantity, inStock);

  if (status === "out_of_stock") {
    return (
      <Badge variant="destructive" className="font-medium">
        <span className="h-1.5 w-1.5 rounded-full bg-destructive animate-pulse" />
        Out of Stock
      </Badge>
    );
  }

  if (status === "low_stock") {
    return (
      <Badge variant="warning" className="font-medium">
        <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-pulse" />
        Only {stockQuantity} left
      </Badge>
    );
  }

  return (
    <Badge variant="success" className="font-medium">
      <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
      In Stock ({stockQuantity})
    </Badge>
  );
}
