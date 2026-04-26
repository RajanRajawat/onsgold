# ONS Gold Full Stack

## Structure

- `app/backend` FastAPI, MongoDB Atlas, Cloudinary, admin auth, products, inquiries, custom requests, analytics
- `app/frontend` static HTML/CSS/JS storefront using the current premium design language

## Main flows

- customer browses products without login
- customer adds products to inquiry cart in browser storage
- inquiry is saved to MongoDB
- customer is redirected to WhatsApp with a prefilled message
- custom jewelry requests accept multiple image uploads through Cloudinary
- admin logs in to manage products and review leads

## Local run

Backend:

```bash
cd app/backend
pip install -r requirements.txt
uvicorn main:app --reload
```

Frontend:

- open `app/frontend/index.html` in a browser
- if needed, set `window.ONS_API_BASE` to your backend URL before loading the page

## Important

- create the first super admin with `python script/super.py`
- configure MongoDB, JWT, SMTP, and Cloudinary using `app/backend/.env.example`
