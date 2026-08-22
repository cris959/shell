-- =================================================================
-- PRE-CARGA DE USUARIOS (Hash BCrypt real de 60 caracteres)
-- =================================================================

INSERT INTO usuarios (id, nombre, email, password, activo)
VALUES
    (1, 'Christian Doe', 'christian.doe@ejemplo.com', '$2a$12$Ui8Oj8K4kw0LcpI8j4.ECeuxSIaMvkG1qv0PXQ3mlBrrcijZKbd9O', true),
    (2, 'Lionel Messi', 'lionel.messi@ejemplo.com', '$2a$12$Ui8Oj8K4kw0LcpI8j4.ECeuxSIaMvkG1qv0PXQ3mlBrrcijZKbd9O', true)
    ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

SELECT setval(pg_get_serial_sequence('usuarios', 'id'), (SELECT COALESCE(MAX(id), 1) FROM usuarios));
-- =================================================================
-- 1. CORREGIR ESTRUCTURA DE TABLA Y REGISTROS PREVIOS
-- =================================================================

UPDATE transacciones
SET activo = true
WHERE activo IS NULL;

ALTER TABLE transacciones
    ALTER COLUMN activo SET DEFAULT true,
ALTER COLUMN activo SET NOT NULL;


-- =================================================================
-- 2. INSERTAR ANÁLISIS CON SUS RECOMENDACIONES (Para Christian Doe - ID 1)
-- =================================================================

WITH nuevo_analisis AS (
INSERT INTO analisis_financiero (
    usuario_id,
    ingreso_mensual,
    nivel_endeudamiento,
    frecuencia_ahorro,
    descripcion,
    valor,
    perfil_financiero,
    probabilidad,
    total_gastado,
    capacidad_ahorro_mensual,
    porcentaje_tasa_ahorro,
    progreso_meta_ahorro,
    meses_para_meta,
    fecha_creacion
) VALUES (
    1, -- Christian Doe
    650000.00,
    2,
    'MENSUAL',
    'Supermercado Coto compras semana',
    42500.00,
    'Moderado',
    0.85,
    42500.00,
    150000.00,
    23.07,
    50.00,
    6.00,
    CURRENT_TIMESTAMP
    ) RETURNING id
    )
INSERT INTO analisis_recomendaciones (analisis_id, recomendacion)
SELECT id, unnest(ARRAY[
                      'Monitorear los gastos recurrentes de supermercado',
                  'Aumentar el margen de ahorro mensual'
                      ])
FROM nuevo_analisis;


-- =================================================================
-- 3. INSERTAR RESUMEN DE GASTOS
-- =================================================================

INSERT INTO analisis_resumen_gastos (analisis_id, categoria, monto)
SELECT id, 'ALIMENTACION', 30000.00 FROM analisis_financiero ORDER BY id DESC LIMIT 1;

INSERT INTO analisis_resumen_gastos (analisis_id, categoria, monto)
SELECT id, 'GASTOS_GENERALES', 12500.00 FROM analisis_financiero ORDER BY id DESC LIMIT 1;


-- =================================================================
-- 4. INSERTAR TRANSACCIONES DE PRUEBA
-- =================================================================

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
VALUES (1, 'Compra en Farmacia', 8500.50, 'EGRESO', 'SALUD', true, CURRENT_TIMESTAMP);

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
VALUES (2, 'Equipamiento Deportivo', 120000.00, 'EGRESO', 'DEPORTES', true, CURRENT_TIMESTAMP);