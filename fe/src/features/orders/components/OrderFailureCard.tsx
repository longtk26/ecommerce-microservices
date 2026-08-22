import * as React from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { XCircle, Store, RotateCcw, AlertTriangle, ShieldAlert } from "lucide-react";
import type { TGetOrderResponse } from "@/types/order";

type OrderFailureCardProps = {
  order: TGetOrderResponse;
};

export function OrderFailureCard({ order }: OrderFailureCardProps) {
  return (
    <Card className="overflow-hidden border-destructive/30 bg-card/80 backdrop-blur-xl shadow-2xl shadow-destructive/10 animate-in shake duration-300">
      <div className="bg-gradient-to-r from-destructive/20 via-destructive/10 to-red-500/20 p-8 text-center border-b border-destructive/20">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-destructive text-white shadow-xl shadow-destructive/30 mb-4">
          <XCircle className="h-9 w-9" />
        </div>
        <Badge variant="destructive" className="mb-2">
          <ShieldAlert className="h-3 w-3" />
          Saga Compensation Executed
        </Badge>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
          Order Cancelled
        </h2>
        <p className="text-xs sm:text-sm text-muted-foreground mt-1 max-w-md mx-auto">
          The transaction could not be completed. Stock was released and any pending charges were refunded.
        </p>
      </div>

      <CardContent className="p-6 sm:p-8 space-y-6">
        <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-4 flex items-start gap-3 text-xs text-destructive">
          <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <p className="font-bold">Possible Reasons for Cancellation:</p>
            <ul className="list-disc list-inside space-y-0.5 text-muted-foreground">
              <li>Insufficient stock inventory reserved concurrently by another buyer</li>
              <li>Simulated payment authorization failure in payment-service</li>
              <li>Saga timeout during event bus choreography</li>
            </ul>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 rounded-xl border border-border/60 bg-secondary/30 p-4 text-xs">
          <div>
            <span className="text-muted-foreground block">Order Reference</span>
            <span className="font-mono font-bold text-foreground truncate block mt-0.5">
              {order.orderId}
            </span>
          </div>
          <div>
            <span className="text-muted-foreground block">Saga Resolution</span>
            <span className="font-bold text-destructive block mt-0.5">CANCELLED</span>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
          <Link href="/" className="w-full">
            <Button variant="outline" className="w-full gap-2">
              <Store className="h-4 w-4" />
              Return to Catalog
            </Button>
          </Link>
          <Link href={`/shops/${order.shopId}`} className="w-full">
            <Button variant="default" className="w-full gap-2">
              <RotateCcw className="h-4 w-4" />
              Try Reordering
            </Button>
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
