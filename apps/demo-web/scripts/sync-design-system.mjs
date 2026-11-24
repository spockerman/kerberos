// Copia o design system (fonte única em /design-system) para dentro do app antes do dev/build.
// A cópia é gerada (ignorada no Git): edite sempre o original.
import { cpSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const source = process.env.DESIGN_SYSTEM_DIR ?? join(here, "..", "..", "..", "design-system");
const target = join(here, "..", "app", "_design-system");

mkdirSync(target, { recursive: true });
for (const item of ["tokens.css", "fonts.css", "fonts", "brand"]) {
    cpSync(join(source, item), join(target, item), { recursive: true });
}
console.log(`design system copiado de ${source}`);
