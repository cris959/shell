-- =================================================================
-- SCRIPT COMPLETO DE INICIALIZACIÓN Y DATOS DE PRUEBA
-- =================================================================

-- (Opcional) Si deseas limpiar la base de datos desde cero antes de correr el script,
-- descomenta y ejecuta la siguiente línea primero:
-- TRUNCATE TABLE analisis_recomendaciones, analisis_resumen_gastos, transacciones, analisis_financiero, usuarios RESTART IDENTITY CASCADE;

-- 1. PRE-CARGA DE USUARIOS
INSERT INTO usuarios (id, nombre, email, password, activo)
VALUES
    (1, 'Christian Doe', 'christian.doe@ejemplo.com', '$2a$12$Ui8Oj8K4kw0LcpI8j4.ECeuxSIaMvkG1qv0PXQ3mlBrrcijZKbd9O', true),
    (2, 'Lionel Messi', 'lionel.messi@ejemplo.com', '$2a$12$Ui8Oj8K4kw0LcpI8j4.ECeuxSIaMvkG1qv0PXQ3mlBrrcijZKbd9O', true)
    ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

SELECT setval(pg_get_serial_sequence('usuarios', 'id'), (SELECT COALESCE(MAX(id), 1) FROM usuarios));

-- 2. AJUSTAR ESTRUCTURA DE TABLA TRANSACCIONES
UPDATE transacciones SET activo = true WHERE activo IS NULL;
ALTER TABLE transacciones ALTER COLUMN activo SET DEFAULT true, ALTER COLUMN activo SET NOT NULL;

-- 3. INSERTAR ANÁLISIS FINANCIERO Y 3 RECOMENDACIONES (Christian Doe)
WITH nuevo_analisis_christian AS (
INSERT INTO analisis_financiero (
    usuario_id, ingreso_mensual, nivel_endeudamiento, frecuencia_ahorro,
    descripcion, valor, perfil_financiero, probabilidad,
    total_gastado, capacidad_ahorro_mensual, porcentaje_tasa_ahorro,
    progreso_meta_ahorro, meses_para_meta, fecha_creacion
) VALUES (
    'christian.doe@ejemplo.com', 650000.00, 2, 'MENSUAL',
    'Análisis financiero general Christian', 42500.00,
    'Moderado', 0.85, 42500.00, 150000.00, 23.07, 50.00, 6.00,
    CURRENT_TIMESTAMP
    ) RETURNING id
    )
INSERT INTO analisis_recomendaciones (analisis_id, recomendacion)
SELECT id, unnest(ARRAY[
                      'Monitorear los gastos recurrentes de supermercado',
                  'Aumentar el margen de ahorro mensual',
                  'Reducir el uso de tarjetas de crédito en compras menores'
                      ])
FROM nuevo_analisis_christian;

-- 4. INSERTAR ANÁLISIS FINANCIERO Y 3 RECOMENDACIONES (Lionel Messi)
WITH nuevo_analisis_messi AS (
INSERT INTO analisis_financiero (
    usuario_id, ingreso_mensual, nivel_endeudamiento, frecuencia_ahorro,
    descripcion, valor, perfil_financiero, probabilidad,
    total_gastado, capacidad_ahorro_mensual, porcentaje_tasa_ahorro,
    progreso_meta_ahorro, meses_para_meta, fecha_creacion
) VALUES (
    'lionel.messi@ejemplo.com', 2500000.00, 1, 'MENSUAL',
    'Análisis financiero general Lionel', 150000.00,
    'Conservador', 0.92, 150000.00, 800000.00, 32.00, 75.00, 4.00,
    CURRENT_TIMESTAMP
    ) RETURNING id
    )
INSERT INTO analisis_recomendaciones (analisis_id, recomendacion)
SELECT id, unnest(ARRAY[
                      'Excelente tasa de ahorro mensual',
                  'Diversificar inversiones a instrumentos de bajo riesgo',
                  'Planificar proyección fiscal y de patrimonio anual'
                      ])
FROM nuevo_analisis_messi;

-- 5. INSERTAR RESUMEN DE GASTOS
INSERT INTO analisis_resumen_gastos (analisis_id, categoria, monto)
SELECT id, 'ALIMENTACION', 30000.00 FROM analisis_financiero WHERE usuario_id = 'christian.doe@ejemplo.com' ORDER BY id DESC LIMIT 1;

INSERT INTO analisis_resumen_gastos (analisis_id, categoria, monto)
SELECT id, 'GASTOS_GENERALES', 12500.00 FROM analisis_financiero WHERE usuario_id = 'christian.doe@ejemplo.com' ORDER BY id DESC LIMIT 1;

INSERT INTO analisis_resumen_gastos (analisis_id, categoria, monto)
SELECT id, 'ALIMENTACION', 100000.00 FROM analisis_financiero WHERE usuario_id = 'lionel.messi@ejemplo.com' ORDER BY id DESC LIMIT 1;

INSERT INTO analisis_resumen_gastos (analisis_id, categoria, monto)
SELECT id, 'SERVICIOS', 50000.00 FROM analisis_financiero WHERE usuario_id = 'lionel.messi@ejemplo.com' ORDER BY id DESC LIMIT 1;

-- 6. INSERTAR 5 TRANSACCIONES PARA CHRISTIAN DOE (Con protección contra duplicados)
INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'christian.doe@ejemplo.com', 'Supermercado Coto compras semana', 42500.00, 'EGRESO', 'ALIMENTACION', true, CURRENT_TIMESTAMP - INTERVAL '1 day'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'christian.doe@ejemplo.com' AND descripcion = 'Supermercado Coto compras semana');

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'christian.doe@ejemplo.com', 'Compra en Farmacia', 8500.50, 'EGRESO', 'SALUD', true, CURRENT_TIMESTAMP - INTERVAL '2 days'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'christian.doe@ejemplo.com' AND descripcion = 'Compra en Farmacia');

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'christian.doe@ejemplo.com', 'Sueldo Mensual', 650000.00, 'INGRESO', 'SALARIO', true, CURRENT_TIMESTAMP - INTERVAL '3 days'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'christian.doe@ejemplo.com' AND descripcion = 'Sueldo Mensual');

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'christian.doe@ejemplo.com', 'Cena Restaurante', 45000.00, 'EGRESO', 'OCIO', true, CURRENT_TIMESTAMP - INTERVAL '4 days'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'christian.doe@ejemplo.com' AND descripcion = 'Cena Restaurante');

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'christian.doe@ejemplo.com', 'Pago de Internet', 18500.00, 'EGRESO', 'SERVICIOS', true, CURRENT_TIMESTAMP - INTERVAL '5 days'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'christian.doe@ejemplo.com' AND descripcion = 'Pago de Internet');

-- 7. INSERTAR 5 TRANSACCIONES PARA LIONEL MESSI (Realistas y protegidas)
INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'lionel.messi@ejemplo.com', 'Supermercado Orgánico Semanal', 115000.00, 'EGRESO', 'ALIMENTACION', true, CURRENT_TIMESTAMP - INTERVAL '1 day'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'lionel.messi@ejemplo.com' AND descripcion = 'Supermercado Orgánico Semanal');

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'lionel.messi@ejemplo.com', 'Carga de Combustible YPF', 45000.00, 'EGRESO', 'TRANSPORTE', true, CURRENT_TIMESTAMP - INTERVAL '2 days'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'lionel.messi@ejemplo.com' AND descripcion = 'Carga de Combustible YPF');

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'lionel.messi@ejemplo.com', 'Honorarios de Asesoría Contable', 95000.00, 'EGRESO', 'SERVICIOS', true, CURRENT_TIMESTAMP - INTERVAL '3 days'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'lionel.messi@ejemplo.com' AND descripcion = 'Honorarios de Asesoría Contable');

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'lionel.messi@ejemplo.com', 'Ingreso por Patrocinio Mensual', 2500000.00, 'INGRESO', 'OTROS', true, CURRENT_TIMESTAMP - INTERVAL '4 days'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'lionel.messi@ejemplo.com' AND descripcion = 'Ingreso por Patrocinio Mensual');

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
SELECT 'lionel.messi@ejemplo.com', 'Suscripción Anual de Streaming y Apps', 24000.00, 'EGRESO', 'OCIO', true, CURRENT_TIMESTAMP - INTERVAL '5 days'
WHERE NOT EXISTS (SELECT 1 FROM transacciones WHERE usuario_id = 'lionel.messi@ejemplo.com' AND descripcion = 'Suscripción Anual de Streaming y Apps');