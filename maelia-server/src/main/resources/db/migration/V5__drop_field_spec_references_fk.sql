-- La colonne references_data_spec est une métadonnée de catalogue (string cross-reference),
-- pas une vraie FK relationnelle. Le seed charge les DataSpec dans l'ordre du JSON,
-- ce qui viole la contrainte quand un FieldSpec pointe vers un DataSpec pas encore inséré.
ALTER TABLE field_spec DROP CONSTRAINT IF EXISTS field_spec_references_data_spec_fkey;
