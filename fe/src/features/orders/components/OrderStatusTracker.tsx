"use client";

import Link from "next/link";
import { useOrderStatus } from "../hooks/use-order-status";
import { useProcessPayment } from "../hooks/use-process-payment";
import { SagaProgressSteps } from "./SagaProgressSteps";
import { OrderCelebrationCard } from "./OrderCelebrationCard";
import { OrderFailureCard } from "./OrderFailureCard";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Container } from "@/components/layout/Container";
import { Stack } from "@/components/layout/Stack";
import { Skeleton } from "@/components/ui/skeleton";
import { formatCurrency } from "@/lib/utils";
import {
  CreditCard,
  AlertCircle,
  RefreshCw,
  Store,
  Sparkles,
  CheckCircle2,
  Package,
} from "lucide-react";

type OrderStatusTrackerProps = {
  orderId: string;
};

export function OrderStatusTracker({ orderId }: OrderStatusTrackerProps) {
  const {
    order,
    status,
    isLoading,
    isError,
    error,
    refetch,
  } = useOrderStatus(orderId);

  const {
    mutate: executePayment,
    isPending: isPaying,
    error: paymentError,
  } = useProcessPayment(orderId);

  if (isLoading) {
    return (
      <Container size="md" className="py-12">
        <Stack gap="lg">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-64 w-full rounded-3xl" />
          <Skeleton className="h-48 w-full rounded-2xl" />
        </Stack>
      </Container>
    );
  }

  if (isError || !order) {
    return (
      <Container size="md" className="py-12">
        <div className="rounded-3xl border border-destructive/30 bg-destructive/10 p-8 text-center space-y-4">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-destructive/20 text-destructive">
            <AlertCircle className="h-7 w-7" />
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-bold text-foreground">Order Not Found</h3>
            <p className="text-xs text-muted-foreground max-w-sm mx-auto">
              {error instanceof Error ? error.message : "Unable to retrieve details for order ID: " + orderId}
            </p>
          </div>
          <div className="flex justify-center gap-3 pt-2">
            <Button variant="outline" onClick={() => refetch()} className="gap-2">
              <RefreshCw className="h-4 w-4" />
              Retry
            </Button>
            <Link href="/">
              <Button variant="default" className="gap-2">
                <Store className="h-4 w-4" />
                Return to Shops
              </Button>
            </Link>
          </div>
        </div>
      </Container>
    );
  }

  return (
    <Container size="md" className="py-10">
      <Stack gap="xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">
              Order Status
            </h1>
            <p className="text-xs text-muted-foreground mt-1 font-mono">
              ID: {order.orderId}
            </p>
          </div>

          <div className="flex items-center gap-2">
            {status === "PENDING" && (
              <Badge variant="warning" className="gap-1.5 px-3 py-1 text-xs">
                <CreditCard className="h-3.5 w-3.5" />
                Awaiting Payment
              </Badge>
            )}
            {status === "COMPLETED" && (
              <Badge variant="success" className="gap-1.5 px-3 py-1 text-xs">
                <Sparkles className="h-3.5 w-3.5" />
                Completed
              </Badge>
            )}
            {status === "CANCELLED" && (
              <Badge variant="destructive" className="gap-1.5 px-3 py-1 text-xs">
                Cancelled
              </Badge>
            )}
          </div>
        </div>

        {/* State Resolution Views */}
        {status === "PENDING" && (
          <Card className="border-indigo-500/40 bg-card/80 backdrop-blur-xl overflow-hidden shadow-2xl shadow-indigo-500/10">
            {/* Header banner */}
            <div className="bg-gradient-to-r from-indigo-500/20 via-purple-500/20 to-pink-500/20 p-6 border-b border-indigo-500/20">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <Badge variant="glow" className="mb-2 text-xs">
                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" />
                    Stock Successfully Reserved
                  </Badge>
                  <h2 className="text-xl font-bold text-foreground">
                    Complete Payment to Finalize Order
                  </h2>
                  <p className="text-xs text-muted-foreground mt-1 max-w-lg">
                    Items are reserved for your order. Click below to complete payment.
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <span className="text-xs text-muted-foreground block">Amount Due</span>
                  <span className="text-2xl font-black text-indigo-400">
                    {formatCurrency(order.totalAmount)}
                  </span>
                </div>
              </div>
            </div>

            <CardContent className="p-6 space-y-6">
              {/* Order Items Preview */}
              <div className="space-y-2.5">
                <h4 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Reserved Items ({order.items.length})
                </h4>
                <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                  {order.items.map((item, idx) => (
                    <div
                      key={idx}
                      className="flex items-center justify-between rounded-xl border border-border/50 bg-secondary/30 p-2.5 text-xs"
                    >
                      <div className="flex items-center gap-2">
                        <div className="h-7 w-7 rounded bg-secondary flex items-center justify-center text-muted-foreground border border-border/40">
                          <Package className="h-3.5 w-3.5" />
                        </div>
                        <span className="font-medium text-foreground">
                          {item.productName} (x{item.quantity})
                        </span>
                      </div>
                      <span className="font-bold text-foreground">
                        {formatCurrency(item.unitPrice * item.quantity)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              {paymentError && (
                <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-4 flex items-center gap-3 text-xs text-destructive">
                  <AlertCircle className="h-5 w-5 shrink-0" />
                  <span>
                    {paymentError instanceof Error
                      ? paymentError.message
                      : "Payment processing failed. Please try again."}
                  </span>
                </div>
              )}

              {/* Action Button */}
              <div className="pt-2">
                <Button
                  onClick={() => executePayment()}
                  isLoading={isPaying}
                  variant="glow"
                  size="lg"
                  className="w-full justify-between"
                >
                  <span className="flex items-center gap-2">
                    <CreditCard className="h-5 w-5" />
                    {isPaying ? "Processing Payment..." : `Pay Now (${formatCurrency(order.totalAmount)})`}
                  </span>
                  <Sparkles className="h-5 w-5" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {status === "COMPLETED" && <OrderCelebrationCard order={order} />}

        {status === "CANCELLED" && <OrderFailureCard order={order} />}

        {/* Timeline visualizer */}
        <SagaProgressSteps status={status || "PENDING"} />
      </Stack>
    </Container>
  );
}

