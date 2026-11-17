import { z } from 'zod'

export const MAX_RECORDS_PER_PUSH = 20

const optionalText = (max: number) =>
  z
    .string()
    .max(max)
    .nullish()
    .transform((v) => v ?? null)

export const recordSchema = z
  .object({
    id: z.string().min(1).max(64),
    name: z.string().trim().min(1).max(200),
    price: z.number().int().min(0),
    photoId: z.string().min(1).max(64).nullish().transform((v) => v ?? null),
    latitude: z.number().min(-90).max(90).nullish().transform((v) => v ?? null),
    longitude: z.number().min(-180).max(180).nullish().transform((v) => v ?? null),
    accuracyM: z.number().min(0).nullish().transform((v) => v ?? null),
    deviceModel: optionalText(200),
    osVersion: optionalText(100),
    appVersion: optionalText(50),
    createdAt: z.number().int().positive(),
    updatedAt: z.number().int().positive(),
    deletedAt: z.number().int().positive().nullish().transform((v) => v ?? null),
  })
  .refine((r) => (r.latitude === null) === (r.longitude === null), {
    message: 'latitude dan longitude harus terisi berpasangan',
  })

export type RecordInput = z.output<typeof recordSchema>

export const pushBodySchema = z.object({
  deviceId: z.string().min(1).max(64),
  records: z.array(z.unknown()).min(1).max(MAX_RECORDS_PER_PUSH),
})

const intParam = (fallback: number, max: number) =>
  z
    .preprocess(
      (v) => (v === undefined || v === null || v === '' ? undefined : Number(v)),
      z.number().int().min(0).max(max).default(fallback),
    )

export const pullQuerySchema = z.object({
  since: intParam(0, Number.MAX_SAFE_INTEGER),
  limit: intParam(200, 200),
})

export const photoIdSchema = z.string().regex(/^[A-Za-z0-9_-]{1,64}$/, 'photoId tidak valid')
