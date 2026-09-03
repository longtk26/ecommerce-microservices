import { CheckCircle2, Clock, XCircle, CreditCard } from "lucide-react";
import type { TOrderStatus } from "@/types/order";

type SagaProgressStepsProps = {
  status: TOrderStatus | string;
};

type Step = {
  id: string;
  name: string;
  description: string;
};

const steps: Step[] = [
  {
    id: "order_created",
    name: "1. Order Created",
    description: "Order has been placed and received",
  },
  {
    id: "stock_reserved",
    name: "2. Stock Reserved",
    description: "Items verified and reserved for your order",
  },
  {
    id: "payment_processed",
    name: "3. Payment Authorization",
    description: "Confirm payment to finalize your purchase",
  },
  {
    id: "order_completed",
    name: "4. Order Completed",
    description: "Order confirmed and preparing for delivery",
  },
];

export function SagaProgressSteps({ status }: SagaProgressStepsProps) {
  const isPending = status === "PENDING";
  const isCompleted = status === "COMPLETED";
  const isCancelled = status === "CANCELLED";

  return (
    <div className="rounded-2xl border border-border/80 bg-card/60 p-6 backdrop-blur-xl space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold text-foreground flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-indigo-400" />
          Order Timeline
        </h3>
      </div>

      <div className="relative space-y-5">
        {steps.map((step, index) => {
          let stepStatus: "completed" | "action_required" | "pending" | "failed" = "pending";

          if (isCompleted) {
            stepStatus = "completed";
          } else if (isCancelled) {
            if (index === 0) stepStatus = "completed";
            else if (index === 1) stepStatus = "completed";
            else stepStatus = "failed";
          } else if (isPending) {
            if (index === 0 || index === 1) {
              stepStatus = "completed"; // Order created and stock reserved
            } else if (index === 2) {
              stepStatus = "action_required"; // Awaiting user payment click
            } else {
              stepStatus = "pending";
            }
          }

          return (
            <div key={step.id} className="relative flex items-start gap-4">
              {/* Connector line */}
              {index < steps.length - 1 && (
                <div
                  className={`absolute left-4 top-8 -bottom-4 w-0.5 ${
                    stepStatus === "completed"
                      ? "bg-emerald-500/50"
                      : stepStatus === "failed"
                      ? "bg-destructive/40"
                      : "bg-border/60"
                  }`}
                />
              )}

              {/* Icon Status */}
              <div className="relative z-10 shrink-0">
                {stepStatus === "completed" && (
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/40">
                    <CheckCircle2 className="h-4 w-4" />
                  </div>
                )}
                {stepStatus === "action_required" && (
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-500/20 text-indigo-400 border border-indigo-500/40 animate-pulse">
                    <CreditCard className="h-4 w-4" />
                  </div>
                )}
                {stepStatus === "pending" && (
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-secondary text-muted-foreground border border-border">
                    <Clock className="h-4 w-4" />
                  </div>
                )}
                {stepStatus === "failed" && (
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-destructive/20 text-destructive border border-destructive/40">
                    <XCircle className="h-4 w-4" />
                  </div>
                )}
              </div>

              {/* Details */}
              <div className="flex flex-1 flex-col pt-0.5">
                <div className="flex items-center justify-between gap-2">
                  <span
                    className={`text-xs font-bold ${
                      stepStatus === "completed"
                        ? "text-emerald-400"
                        : stepStatus === "action_required"
                        ? "text-indigo-300 font-extrabold"
                        : stepStatus === "failed"
                        ? "text-destructive"
                        : "text-muted-foreground"
                    }`}
                  >
                    {step.name}
                    {stepStatus === "action_required" && (
                      <span className="ml-2 text-[10px] text-amber-400 bg-amber-500/10 border border-amber-500/30 px-2 py-0.5 rounded-full font-medium">
                        Action Required
                      </span>
                    )}
                  </span>
                </div>
                <p className="text-[11px] text-muted-foreground mt-0.5">
                  {step.description}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

