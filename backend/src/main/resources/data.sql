-- =================================================================
-- 1. CORREGIR ESTRUCTURA DE TABLA Y REGISTROS PREVIOS
-- =================================================================

-- Actualizar registros existentes que quedaron en NULL
UPDATE transacciones
SET activo = true
WHERE activo IS NULL;

-- Asegurar restricción NOT NULL y DEFAULT true para transacciones
ALTER TABLE transacciones
    ALTER COLUMN activo SET DEFAULT true,
ALTER COLUMN activo SET NOT NULL;


-- =================================================================
-- 2. INSERTAR ANÁLISIS CON SUS RECOMENDACIONES
-- (Alineado estrictamente a AnalisisFinanciero.java)
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
    'USR-1001',
    650000.00,
    2,
    'MENSUAL',
    'Supermercado Coto compras semana',
    42500.00,
    'Moderado',
    0.85,
    42500.00,   -- total_gastado
    150000.00,  -- capacidad_ahorro_mensual
    23.07,      -- porcentaje_tasa_ahorro
    50.00,      -- progreso_meta_ahorro
    6.00,       -- meses_para_meta
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
-- 3. INSERTAR RESUMEN DE GASTOS (Map<String, Double> en Java)
-- =================================================================

INSERT INTO analisis_resumen_gastos (analisis_id, categoria, monto)
VALUES
    (1, 'ALIMENTACION', 30000.00),
    (1, 'GASTOS_GENERALES', 12500.00)
    ON CONFLICT DO NOTHING;

-- =================================================================
-- 4. INSERTAR TRANSACCIÓN DE PRUEBA
-- =================================================================

INSERT INTO transacciones (usuario_id, descripcion, monto, tipo, categoria, activo, fecha_transaccion)
VALUES ('USR-1001', 'Compra en Farmacia', 8500.50, 'EGRESO', 'Salud', true, CURRENT_TIMESTAMP);