// Identidade do sistema. Única fonte: design-system/brand/. Trocar a marca não exige mudar este código.
import brandJson from "../../design-system/brand/brand.json";
import logoUrl from "../../design-system/brand/logo.svg";
import logoMarkUrl from "../../design-system/brand/logo-mark.svg";

export const brand = {
    productName: brandJson.productName,
    isPlaceholder: brandJson.placeholder === true,
    logoUrl,
    logoMarkUrl
};
