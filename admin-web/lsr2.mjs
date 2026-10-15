import fs from 'node:fs';
import { S3Client, ListObjectsV2Command } from '@aws-sdk/client-s3';
const env = Object.fromEntries(fs.readFileSync('.env','utf8').split('\n')
  .map(l=>l.trim()).filter(l=>l && !l.startsWith('#'))
  .map(l=>{const i=l.indexOf('='); return [l.slice(0,i).trim(), l.slice(i+1).trim().replace(/^["']|["']$/g,'')];}));
const s3 = new S3Client({ region:'auto', endpoint: env.R2_ENDPOINT,
  credentials:{ accessKeyId: env.R2_ACCESS_KEY_ID, secretAccessKey: env.R2_SECRET_ACCESS_KEY }});
const out = await s3.send(new ListObjectsV2Command({ Bucket: env.R2_BUCKET, Prefix: 'banners/' }));
const rows = (out.Contents||[]).sort((a,b)=>b.LastModified-a.LastModified).slice(0,15);
for (const r of rows) console.log(r.LastModified.toISOString(), `${env.R2_PUBLIC_BASE_URL}/${r.Key}`);
