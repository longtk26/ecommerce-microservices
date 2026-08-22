import * as React from "react";
import { formatCurrency } from "@/lib/utils";

type CartSummaryProps = {
  itemCount: number;
  subtotal: number;
  total: number;
};

export function CartSummary({ itemCount, subtotal, total }: CartSummaryProps) {
  return (
    <div className="space-y-2.5 rounded-xl border border-border/80 bg-secondary/30 p-4 text-xs">
      <div className="flex items-center justify-between text-muted-foreground">
        <span>Items ({itemCount})</span>
        <span>{formatCurrency(subtotal)}</span>
      </div>

      <div className="flex items-center justify-between text-muted-foreground">
        <span>Estimated Delivery</span>
        <span className="text-emerald-400 font-medium">Free</span>
      </div>

      <div className="border-t border-border/60 pt-2 flex items-center justify-between font-bold text-sm text-foreground">
        <span>Total</span>
        <span className="text-base font-black text-indigo-400">
          {formatCurrency(total)}
        </span>
      </div>
    </div>
  );
}
