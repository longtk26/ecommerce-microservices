"use client";

import * as React from "react";
import Link from "next/link";
import { useCheckoutForm } from "../hooks/use-checkout-form";
import { CheckoutSummaryCard } from "./CheckoutSummaryCard";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Container } from "@/components/layout/Container";
import { Stack } from "@/components/layout/Stack";
import {
  User,
  AlertCircle,
  ArrowRight,
  Sparkles,
  ShoppingBag,
  Store,
  CreditCard,
} from "lucide-react";

export function CheckoutForm() {
  const {
    form,
    onSubmit,
    isSubmitting,
    errorMessage,
    computedUserId,
    items,
    shopName,
    isEmpty,
  } = useCheckoutForm();

  if (isEmpty) {
    return (
      <Container size="md" className="py-16">
        <div className="rounded-3xl border border-border bg-card/60 p-12 text-center space-y-4 backdrop-blur-xl">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-secondary text-muted-foreground">
            <ShoppingBag className="h-8 w-8" />
          </div>
          <div className="space-y-1">
            <h2 className="text-xl font-bold text-foreground">Your cart is empty</h2>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              You do not have any items in your checkout session. Return to a shop to add items.
            </p>
          </div>
          <Link href="/">
            <Button variant="default" className="gap-2 mt-2">
              <Store className="h-4 w-4" />
              Browse Shops
            </Button>
          </Link>
        </div>
      </Container>
    );
  }

  return (
    <Container size="xl" className="py-10">
      <Stack gap="xl">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-foreground">
            Order Checkout
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Complete your customer details to trigger the order saga transaction.
          </p>
        </div>

        {errorMessage && (
          <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-4 flex items-center gap-3 text-sm text-destructive">
            <AlertCircle className="h-5 w-5 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Form Column */}
          <div className="lg:col-span-7 space-y-6">
            <Card className="border-border/80 bg-card/60 backdrop-blur-xl">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <User className="h-5 w-5 text-indigo-400" />
                  <CardTitle className="text-lg">Customer Information</CardTitle>
                </div>
                <CardDescription>
                  Enter your name. This will automatically format your user identity string.
                </CardDescription>
              </CardHeader>

              <CardContent>
                <form onSubmit={onSubmit} className="space-y-6">
                  <div className="space-y-2">
                    <label
                      htmlFor="customerName"
                      className="text-xs font-bold uppercase tracking-wider text-muted-foreground"
                    >
                      Full Name
                    </label>
                    <Input
                      id="customerName"
                      placeholder="e.g. Alex Mercer"
                      {...form.register("customerName")}
                      className={
                        form.formState.errors.customerName
                          ? "border-destructive focus-visible:ring-destructive"
                          : ""
                      }
                    />
                    {form.formState.errors.customerName && (
                      <p className="text-xs text-destructive">
                        {form.formState.errors.customerName.message}
                      </p>
                    )}
                  </div>

                  {/* Computed User ID Preview */}
                  <div className="rounded-xl border border-border/60 bg-secondary/40 p-4 space-y-2">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-muted-foreground font-medium">
                        Computed Backend User ID:
                      </span>
                      <Badge variant="outline" className="font-mono text-indigo-300 border-indigo-500/30 bg-indigo-500/10">
                        {computedUserId}
                      </Badge>
                    </div>
                    <p className="text-[11px] text-muted-foreground leading-relaxed">
                      Matches the backend contract format (<code>userId: &quot;user-[name]&quot;</code>) transmitted to the Order Service.
                    </p>
                  </div>

                  {/* Payment Info Note */}
                  <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-4 flex items-center gap-3 text-xs text-emerald-300">
                    <CreditCard className="h-5 w-5 shrink-0" />
                    <div>
                      <p className="font-bold">Test Payment Gateway Integration</p>
                      <p className="text-emerald-400/80 mt-0.5">
                        Payment will be automatically orchestrated via payment-service and RabbitMQ saga events.
                      </p>
                    </div>
                  </div>

                  <Button
                    type="submit"
                    variant="glow"
                    size="lg"
                    isLoading={isSubmitting}
                    className="w-full justify-between mt-4"
                  >
                    <span className="flex items-center gap-2">
                      <Sparkles className="h-5 w-5" />
                      Place Order & Start Saga
                    </span>
                    <ArrowRight className="h-5 w-5" />
                  </Button>
                </form>
              </CardContent>
            </Card>
          </div>

          {/* Summary Column */}
          <div className="lg:col-span-5">
            <CheckoutSummaryCard items={items} shopName={shopName} />
          </div>
        </div>
      </Stack>
    </Container>
  );
}
