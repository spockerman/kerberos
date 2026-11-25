import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
    title: "Portal de Provisionamento — Central 156",
    description: "Cria sistemas e clientes na plataforma de identidade"
};

export default function RootLayout({ children }: { children: ReactNode }) {
    return (
        <html lang="pt-BR">
            <body>{children}</body>
        </html>
    );
}
