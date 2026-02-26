# Loretta Bank Frontend (Next.js 14)

Production-ready banking UI for Loretta Bank, built with Next.js 14 App Router, TypeScript, Tailwind CSS, TanStack Query, Axios, and Zustand.

## Features

- Secure auth session handling with cookie-based JWT storage fallback
- Role-based views for `ADMIN`, `CUSTOMER`, and `SUPPORT`
- Protected routes via Next.js middleware
- Dashboard with balance, accounts, and recent transactions
- Accounts listing + account detail view + admin account creation
- Transfer flow with validation + `Idempotency-Key` support
- Paginated transactions with filters
- Admin panel with customers and audit summary
- Axios interceptors for auth token injection and 401 auto logout
- Loading skeletons, toast notifications, and global error boundaries

## Tech Stack

- Next.js 14+ (App Router)
- TypeScript
- TailwindCSS
- TanStack Query
- Axios
- Zustand
- ESLint + Prettier

## Environment

Copy `.env.example` to `.env.local`:

```bash
cp .env.example .env.local
```

Set your backend gateway URL:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Local Development

```bash
npm install
npm run dev
```

App runs at `http://localhost:3000` by default.

If `3000` is already in use:

```bash
npm run dev:3001
```

## Production Build

```bash
npm run build
npm run start
```

If `3000` is in use for production start:

```bash
npm run start:3001
```

## Docker

Build the image:

```bash
docker build -t loretta-bank-client .
```

Run container:

```bash
docker run --rm -p 3000:3000 --env NEXT_PUBLIC_API_URL=http://host.docker.internal:8080 loretta-bank-client
```

## API Compatibility

The frontend is wired to the Loretta Bank backend routes:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `GET /api/v1/customers/me`
- `GET /api/v1/accounts/customer/{customerId}`
- `POST /api/v1/accounts`
- `GET /api/v1/transactions/account/{accountId}`
- `POST /api/v1/transfers`
- `GET /api/v1/customers`

## Quality Commands

```bash
npm run lint
npm run typecheck
npm run format
```

