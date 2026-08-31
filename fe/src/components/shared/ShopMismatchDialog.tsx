"use client";

import { useCartStore } from "@/store/cart-store";
import { Button } from "@/components/ui/button";
import { AlertTriangle } from "lucide-react";

export function ShopMismatchDialog() {
  const pendingShopChange = useCartStore((state) => state.pendingShopChange);
  const currentShopName = useCartStore((state) => state.shopName);
  const confirmShopChange = useCartStore((state) => state.confirmShopChange);
  const cancelShopChange = useCartStore((state) => state.cancelShopChange);

  if (!pendingShopChange) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="w-full max-w-md rounded-2xl border border-amber-500/30 bg-card p-6 shadow-2xl shadow-amber-500/10">
        <div className="flex items-center gap-3 text-amber-400">
          <div className="rounded-full bg-amber-500/15 p-2.5">
            <AlertTriangle className="h-6 w-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">Switch Shop Cart?</h3>
        </div>

        <p className="mt-3 text-sm text-muted-foreground leading-relaxed">
          Your cart currently contains items from <strong className="text-foreground">{currentShopName || "another shop"}</strong>.
          An order can only contain items from one shop at a time.
        </p>

        <p className="mt-2 text-sm text-muted-foreground">
          Do you want to clear your current cart and start shopping at <strong className="text-primary">{pendingShopChange.shopName}</strong>?
        </p>

        <div className="mt-6 flex items-center justify-end gap-3">
          <Button variant="outline" onClick={cancelShopChange}>
            Keep Current Cart
          </Button>
          <Button variant="destructive" onClick={confirmShopChange}>
            Clear & Switch
          </Button>
        </div>
      </div>
    </div>
  );
}
