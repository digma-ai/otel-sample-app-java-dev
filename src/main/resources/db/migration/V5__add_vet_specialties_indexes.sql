CREATE INDEX IF NOT EXISTS idx_vet_specialties_vet_id ON vet_specialties (vet_id);
CREATE INDEX IF NOT EXISTS idx_vet_specialties_specialty_id ON vet_specialties (specialty_id);
CREATE INDEX IF NOT EXISTS idx_specialties_id ON specialties (id);