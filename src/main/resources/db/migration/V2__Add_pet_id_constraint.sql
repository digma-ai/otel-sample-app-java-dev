-- Add unique constraint on pet_id
ALTER TABLE pets ADD CONSTRAINT uk_pet_id UNIQUE (id);