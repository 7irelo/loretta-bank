import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export default async function HomePage() {
  redirect((await cookies()).get("lb_user")?.value ? "/dashboard" : "/login");
}
