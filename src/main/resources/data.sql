INSERT IGNORE INTO Usuario(id, email, password, rol, activo) VALUES(1, 'test@unlam.edu.ar', '$2a$10$oFRmDowvSyPXCiu1ZohdkuqywsVBsb.WZgeFqFcdtgmQBYS9EVNkG', 'ADMIN', true);

-- Planes (nivel 1 = Básico, 2 = Premium)
INSERT IGNORE INTO Plan(id, nombre, descripcion, precio, nivel) VALUES
(1, 'Plan Básico', 'Prevención y atención primaria. 1 consulta mensual cubierta al 100%.', 20000, 1),
(2, 'Plan Premium', 'Cobertura de alta complejidad y bienestar integral. Consultas ilimitadas.', 50000, 2);

-- Servicios: nivelRequerido indica el plan mínimo que los cubre
INSERT IGNORE INTO Servicio(id, nombre, descripcion, nivelRequerido) VALUES
(1, 'Consulta clínica', 'Atención primaria, chequeo de rutina y control de peso.', 1),
(2, 'Vacunación', 'Antirrábica y séxtuple (perros) / triple felina (gatos).', 1),
(3, 'Desparasitación', 'Control parasitario interno y externo.', 1),
(4, 'Control preventivo', 'Revisión general periódica.', 1),
(5, 'Corte de uñas', 'Cuidado higiénico básico en consulta.', 1),
(6, 'Limpieza de oídos', 'Cuidado higiénico básico en consulta.', 1),
(7, 'Dermatología', 'Consulta con especialista en piel.', 2),
(8, 'Cardiología', 'Consulta con especialista y estudios cardíacos.', 2),
(9, 'Traumatología', 'Consulta con especialista en huesos y articulaciones.', 2),
(10, 'Análisis clínicos', 'Hemograma, bioquímica y análisis de orina.', 2),
(11, 'Radiografía', 'Diagnóstico por imágenes con rayos X.', 2),
(12, 'Ecografía', 'Ecografía abdominal.', 2),
(13, 'Cirugía', 'Quirófano, anestesia e intervenciones programadas.', 2),
(14, 'Internación', 'Estadía 24 hs bajo monitoreo veterinario.', 2),
(15, 'Guardia 24 hs', 'Atención de urgencias a toda hora.', 2);

-- Veterinarias reales de Buenos Aires. El teléfono queda vacío cuando el directorio no lo publica.
-- CUIT de placeholder: no es el de la clínica. Horario de turnos 09 a 18, salvo San Justo y Leocan (09 a 21).
INSERT IGNORE INTO Veterinaria(id, nombre, direccion, telefono, cuit, horaApertura, horaCierre) VALUES
(1, 'Clínica Veterinaria San Justo', 'República de Chile 2915, San Justo', '4651-4853', '30-70000001', '09:00:00', '21:00:00'),
(2, 'Centro Integral Veterinario Del Alto', 'Guise 1870, Palermo', '11 3856-1688', '30-70000002', '09:00:00', '18:00:00'),
(3, 'A Cara de Perro', '3 de Febrero 1130, Palermo', '11 2501-6914', '30-70000003', '09:00:00', '18:00:00'),
(4, 'Dinamita y Volador', 'Billinghurst 1464, Palermo', '11 2035-6616', '30-70000004', '09:00:00', '18:00:00'),
(5, 'Veterinaria de Melo', 'Pacheco de Melo 2718, Palermo', '11 4800-1635', '30-70000005', '09:00:00', '18:00:00'),
(6, 'Veterinaria Dr. Paludi', 'Gorriti 3901, Palermo', '', '30-70000006', '09:00:00', '18:00:00'),
(7, 'Centro Médico Veterinario', 'Guatemala 5491, Palermo', '', '30-70000007', '09:00:00', '18:00:00'),
(8, 'Animal Vet', 'Av. Monroe 2885, Belgrano', '11 4543-7741', '30-70000008', '09:00:00', '18:00:00'),
(9, 'Animed', 'Vidal 2301, Belgrano', '11 4784-0181', '30-70000009', '09:00:00', '18:00:00'),
(10, 'Belgrano Veterinaria Centro Médico', 'Amenábar 2214, Belgrano', '11 4782-9079', '30-70000010', '09:00:00', '18:00:00'),
(11, 'Leocan', 'Manuel Ugarte 2226, Belgrano', '11 4785-5566', '30-70000011', '09:00:00', '21:00:00'),
(12, 'Veterinaria Whippet', 'Arcos 2510, Belgrano', '11 2261-6733', '30-70000012', '09:00:00', '18:00:00'),
(13, 'Felicidad', 'Blanco Encalada 2096, Belgrano', '', '30-70000013', '09:00:00', '18:00:00'),
(14, 'Animals', 'Thompson 598, Caballito', '11 4431-1487', '30-70000014', '09:00:00', '18:00:00'),
(15, 'Centro Médico Veterinario Delfis', 'Av. José María Moreno 515, Caballito', '', '30-70000015', '09:00:00', '18:00:00'),
(16, 'Veterinaria Life', 'Av. Díaz Vélez 5506, Caballito', '', '30-70000016', '09:00:00', '18:00:00'),
(17, 'San Marcos', 'Av. Díaz Vélez 4730, Caballito', '', '30-70000017', '09:00:00', '18:00:00'),
(18, 'CEMV', 'Av. Córdoba 2663, Recoleta', '11 4962-9037', '30-70000018', '09:00:00', '18:00:00'),
(19, 'Agroveterinaria Fátima', 'Mansilla 2688, Recoleta', '11 4963-7713', '30-70000019', '09:00:00', '18:00:00'),
(20, 'Veterinaria Patán', 'French 2741, Recoleta', '', '30-70000020', '09:00:00', '18:00:00'),
(21, 'Veterinaria Patagonia', 'Sánchez de Bustamante 2288, Recoleta', '11 5658-1999', '30-70000021', '09:00:00', '18:00:00'),
(22, 'La Unión', 'Billinghurst 1334, Recoleta', '11 4963-8588', '30-70000022', '09:00:00', '18:00:00'),
(23, 'Veterinaria Stephanus', 'French 3032, Recoleta', '11 5636-2450', '30-70000023', '09:00:00', '18:00:00'),
(24, 'Clínica Veterinaria Recoleta', 'French 2778, Recoleta', '11 5464-1241', '30-70000024', '09:00:00', '18:00:00'),
(25, 'Veterinaria Juncal', 'Juncal 2263, Recoleta', '', '30-70000025', '09:00:00', '18:00:00'),
(26, 'Clínica Talita', 'Arenales 1856, Recoleta', '', '30-70000026', '09:00:00', '18:00:00'),
(27, 'Vetnova', 'Marcelo T. de Alvear 1615, Recoleta', '', '30-70000027', '09:00:00', '18:00:00'),
(28, 'Veterinaria Bonaire', 'Junín 654, Recoleta', '', '30-70000028', '09:00:00', '18:00:00'),
(29, 'The Animal Center', 'Laprida 1331, Recoleta', '', '30-70000029', '09:00:00', '18:00:00'),
(30, 'Veterinaria Merli', 'Cnel. Apolinario Figueroa 299, Villa Crespo', '', '30-70000030', '09:00:00', '18:00:00');

-- INSERT IGNORE no corrige las 4 clínicas ficticias que ya puedan estar cargadas.
UPDATE Veterinaria SET nombre = 'Clínica Veterinaria San Justo', direccion = 'República de Chile 2915, San Justo', telefono = '4651-4853', cuit = '30-70000001', horaApertura = '09:00:00', horaCierre = '21:00:00' WHERE id = 1;
UPDATE Veterinaria SET nombre = 'Centro Integral Veterinario Del Alto', direccion = 'Guise 1870, Palermo', telefono = '11 3856-1688', cuit = '30-70000002', horaApertura = '09:00:00', horaCierre = '18:00:00' WHERE id = 2;
UPDATE Veterinaria SET nombre = 'A Cara de Perro', direccion = '3 de Febrero 1130, Palermo', telefono = '11 2501-6914', cuit = '30-70000003', horaApertura = '09:00:00', horaCierre = '18:00:00' WHERE id = 3;
UPDATE Veterinaria SET nombre = 'Dinamita y Volador', direccion = 'Billinghurst 1464, Palermo', telefono = '11 2035-6616', cuit = '30-70000004', horaApertura = '09:00:00', horaCierre = '18:00:00' WHERE id = 4;

INSERT IGNORE INTO Veterinaria_Servicio(veterinaria_id, servicio_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 5), (1, 6), (1, 7),
(2, 1), (2, 2), (2, 4), (2, 10), (2, 11),
(3, 1), (3, 3), (3, 4), (3, 8), (3, 9), (3, 12),
(4, 1), (4, 2), (4, 10), (4, 11), (4, 12), (4, 13), (4, 14), (4, 15),
(5, 1), (5, 4), (5, 5), (5, 6), (5, 7), (5, 10),
(6, 1), (6, 2), (6, 3), (6, 5), (6, 6), (6, 7),
(7, 1), (7, 2), (7, 4), (7, 10), (7, 11),
(8, 1), (8, 3), (8, 4), (8, 8), (8, 9), (8, 12),
(9, 1), (9, 2), (9, 10), (9, 11), (9, 12), (9, 13), (9, 14), (9, 15),
(10, 1), (10, 4), (10, 5), (10, 6), (10, 7), (10, 10),
(11, 1), (11, 2), (11, 3), (11, 5), (11, 6), (11, 7),
(12, 1), (12, 2), (12, 4), (12, 10), (12, 11),
(13, 1), (13, 3), (13, 4), (13, 8), (13, 9), (13, 12),
(14, 1), (14, 2), (14, 10), (14, 11), (14, 12), (14, 13), (14, 14), (14, 15),
(15, 1), (15, 4), (15, 5), (15, 6), (15, 7), (15, 10),
(16, 1), (16, 2), (16, 3), (16, 5), (16, 6), (16, 7),
(17, 1), (17, 2), (17, 4), (17, 10), (17, 11),
(18, 1), (18, 3), (18, 4), (18, 8), (18, 9), (18, 12),
(19, 1), (19, 2), (19, 10), (19, 11), (19, 12), (19, 13), (19, 14), (19, 15),
(20, 1), (20, 4), (20, 5), (20, 6), (20, 7), (20, 10),
(21, 1), (21, 2), (21, 3), (21, 5), (21, 6), (21, 7),
(22, 1), (22, 2), (22, 4), (22, 10), (22, 11),
(23, 1), (23, 3), (23, 4), (23, 8), (23, 9), (23, 12),
(24, 1), (24, 2), (24, 10), (24, 11), (24, 12), (24, 13), (24, 14), (24, 15),
(25, 1), (25, 4), (25, 5), (25, 6), (25, 7), (25, 10),
(26, 1), (26, 2), (26, 3), (26, 5), (26, 6), (26, 7),
(27, 1), (27, 2), (27, 4), (27, 10), (27, 11),
(28, 1), (28, 3), (28, 4), (28, 8), (28, 9), (28, 12),
(29, 1), (29, 2), (29, 10), (29, 11), (29, 12), (29, 13), (29, 14), (29, 15),
(30, 1), (30, 4), (30, 5), (30, 6), (30, 7), (30, 10);

-- Socio de demo: socio@medipet.com / 1234
INSERT IGNORE INTO Usuario(id, email, password, rol, activo) VALUES(2, 'socio@medipet.com', '$2a$10$aOHU3dE4FX55d2HMTvXS2Oqrp1phohqusFMmt0R1j6X4w2eK.wzO.', 'SOCIO', true);
-- Veterinaria de demo: vet@medipet.com / 1234, ligada a Clínica Veterinaria San Justo
INSERT IGNORE INTO Usuario(id, email, password, rol, activo) VALUES(3, 'vet@medipet.com', '$2a$10$aOHU3dE4FX55d2HMTvXS2Oqrp1phohqusFMmt0R1j6X4w2eK.wzO.', 'VETERINARIA', true);
UPDATE Veterinaria SET usuario_id = 3 WHERE id = 1 AND usuario_id IS NULL;
INSERT IGNORE INTO Socio(id, nombre, apellido, dni, telefono, usuario_id) VALUES(1, 'Lucía', 'Gómez', '35111222', '1155554444', 2);
INSERT IGNORE INTO Mascota(id, nombre, especie, raza, fechaNacimiento, peso, socio_id, plan_id) VALUES

(1, 'Firulais', 'PERRO', 'Mestizo', '2019-04-10', 18.5, 1, 1),
(2, 'Michi', 'GATO', 'Siamés', '2021-08-22', 4.2, 1, 2);
