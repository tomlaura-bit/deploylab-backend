ALTER TABLE scenario ADD COLUMN skill_id UUID REFERENCES skill(id);
UPDATE scenario SET skill_id=(SELECT id FROM skill WHERE name=CASE scenario.template_key
 WHEN 'API_URL' THEN 'Diagnóstico de APIs'
 WHEN 'DB_AUTH' THEN 'Bases de datos'
 WHEN 'PERMISSIONS' THEN 'Autorización' END);
