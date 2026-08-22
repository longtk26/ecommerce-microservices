import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";
import { Header } from "@/components/shared/Header";
import { Footer } from "@/components/shared/Footer";
import { CartDrawer } from "@/features/cart/components/CartDrawer";
import { ShopMismatchDialog } from "@/components/shared/ShopMismatchDialog";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-sans",
});

export const metadata: Metadata = {
  title: "ShopSaga — Distributed Microservices Commerce",
  description:
    "Real-time e-commerce storefront powered by Spring Boot event choreography, RabbitMQ sagas, and Next.js.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark">
      <body className={`${inter.variable} font-sans antialiased min-h-screen flex flex-col`}>
        <Providers>
          <Header />
          <main className="flex-1 pb-16">{children}</main>
          <CartDrawer />
          <ShopMismatchDialog />
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
