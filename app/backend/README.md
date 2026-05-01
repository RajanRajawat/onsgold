# ONS Gold Backend

FastAPI backend for a wholesale jewelry catalog and inquiry platform.

## Included

- JWT-protected admin auth
- Products CRUD
- public product search and filters
- WhatsApp inquiry flow
- custom jewelry request flow
- Cloudinary image uploads
- email notifications to super admins
- dashboard analytics
- CSV inquiry export
- Swagger/OpenAPI docs

## Setup

```bash
pip install -r requirements.txt
```

Copy `.env.example` to `.env` and fill MongoDB, JWT, SMTP, and Cloudinary values.

## Run

```bash
uvicorn main:app --reload
```

## Bootstrap first super admin

```bash
python script/super.py
```

## Main API groups

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/products`
- `GET /api/v1/products`
- `GET /api/v1/products/random`
- `POST /api/v1/orders`
- `POST /api/v1/custom-requests`
- `POST /api/v1/uploads/product-images`
- `POST /api/v1/uploads/custom-request-images`
- `GET /api/v1/admin/orders`
- `GET /api/v1/admin/custom-requests`
- `GET /api/v1/dashboard/summary`

## Frontend

The matching static frontend is in `../frontend/index.html`.

- Uses Axios to call the API
- stores the inquiry cart in `localStorage`
- supports custom request image uploads
- includes an admin dashboard section for product and inquiry management

## Deployment

Backend:

- Render or Railway
- start command: `uvicorn main:app --host 0.0.0.0 --port 10000`

Frontend:

- Vercel or Netlify
- set `window.ONS_API_BASE` if the backend URL differs from `https://onsgold.onrender.com/api/v1`

Services:

- MongoDB Atlas free tier
- Cloudinary free tier
- SMTP provider or Resend-compatible SMTP relay

## Notes

- Customer flows are intentionally inquiry-based only.
- Every inquiry is stored before WhatsApp redirect.
- If SMTP is not configured, email logs are printed to the console.
- The built-in rate limiter is in-memory and good for starter deployments. Move it to Redis for multi-instance production.
