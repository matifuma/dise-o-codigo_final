INSERT INTO "TipoCompetencia" SELECT t,t FROM unnest(ARRAY['TECNICA','ACTITUDINAL']) t;
INSERT INTO "TipoPregunta" SELECT t,t FROM unnest(ARRAY['OPCION_MULTIPLE','VERDADERO_FALSO','ESCALA']) t;
INSERT INTO "EstadoCuestionario" SELECT t,t FROM unnest(ARRAY['PENDIENTE','ACTIVO','EN_CURSO','COMPLETADO','INCOMPLETO','SIN_CONTESTAR']) t;
INSERT INTO "Consultor" ("nombreUsuario","tipoAcceso") VALUES ('CONSULTOR','CONSULTOR');
INSERT INTO "ConfiguracionSistema" VALUES (1,'Respondé todas las preguntas de cada bloque antes de avanzar. Los bloques confirmados quedan cerrados. Podés salir y retomar con la misma clave.',60,3);
INSERT INTO "Empresa" (nombre) SELECT nombre FROM empresa ORDER BY nombre;
INSERT INTO "Puesto" SELECT p.codigo,p.nombre,p.descripcion,p.activo,e."idEmpresa" FROM puesto p JOIN "Empresa" e ON e.nombre=p.empresa;
INSERT INTO "Competencia" SELECT id,nombre,descripcion,activo,tipo,tecnologia,comportamiento FROM competencia;
INSERT INTO requiere SELECT puesto,competencia,minimo FROM requisito;
INSERT INTO "Factor" SELECT id,competencia,nombre,descripcion,orden,activo FROM factor;
INSERT INTO "Pregunta" SELECT id,factor,tipo,nombre,enunciado,descripcion,activo FROM pregunta;
INSERT INTO "PlantillaOpcion" SELECT id,tipo,texto,puntaje,orden,activo FROM opcion;
INSERT INTO "OpcionRespuesta" ("idPregunta","idPlantilla",texto,puntaje,orden,activo)
 SELECT p.pregunta,o.id,o.texto,p.puntaje,o.orden,o.activo FROM pregunta_opcion p JOIN opcion o ON o.id=p.opcion;
INSERT INTO "Candidato" SELECT numero,apellido,nombre,tipo_doc,documento,nacimiento,genero,nacionalidad,escolaridad,email,activo,eliminado FROM candidato;
INSERT INTO "RegistroAuditoria" ("idRegistroAuditoria","fechaHora","detalleOperacion","entidadAfectada") SELECT id,fecha,detalle,entidad FROM auditoria;
INSERT INTO "Evaluacion" ("idEvaluacion",codigo,fecha) SELECT id,puesto,fecha FROM evaluacion;
INSERT INTO "Cuestionario" ("idCuestionario","idEvaluacion","nroCandidato",clave,"fechaCreacion","idEstadoCuestionario","fechaInicio","fechaFin","ultimoIngreso","cantAccesos",posicion,"fechaCambioEstado")
 SELECT id,evaluacion,candidato,clave,creado,estado,inicio,fin,ultimo,accesos,posicion,
 CASE estado WHEN 'ACTIVO' THEN creado WHEN 'EN_CURSO' THEN inicio WHEN 'COMPLETADO' THEN fin END FROM cuestionario;
INSERT INTO "BloqueCuestionario" ("idCuestionario",numero,completado,"fechaCompletado")
 SELECT i.cuestionario,i.orden/2+1,max(i.orden)<q.posicion,CASE WHEN q.estado='COMPLETADO' AND max(i.orden)=q.posicion-1 THEN q.fin END
 FROM item i JOIN cuestionario q ON q.id=i.cuestionario GROUP BY i.cuestionario,i.orden/2,q.posicion,q.estado,q.fin;
INSERT INTO incluye SELECT i.cuestionario,i.orden,b."idBloqueCuestionario",i.orden%2+1,
 (SELECT CASE WHEN count(*)=1 THEN min(id) END FROM pregunta p WHERE p.factor=i.factor AND p.enunciado=i.enunciado),
 i.factor,i.enunciado FROM item i JOIN "BloqueCuestionario" b ON b."idCuestionario"=i.cuestionario AND b.numero=i.orden/2+1;
-- Recuperar opciones históricas retiradas del catálogo cuando su pregunta es identificable.
INSERT INTO "OpcionRespuesta" ("idPregunta","idPlantilla",texto,puntaje,orden,activo)
 SELECT DISTINCT ON (i."idPregunta",a.opcion) i."idPregunta",a.opcion,a.texto,a.puntaje,a.orden,false
 FROM alternativa a JOIN incluye i ON (i."idCuestionario",i."ordenGlobal")=(a.cuestionario,a.item)
 JOIN "PlantillaOpcion" p ON p."idPlantilla"=a.opcion WHERE i."idPregunta" IS NOT NULL
 ORDER BY i."idPregunta",a.opcion,a.cuestionario DESC ON CONFLICT DO NOTHING;
INSERT INTO "AlternativaEmitida" SELECT a.cuestionario,a.item,a.opcion,a.texto,a.puntaje,a.orden,o."idOpcionRespuesta"
 FROM alternativa a JOIN incluye i ON (i."idCuestionario",i."ordenGlobal")=(a.cuestionario,a.item)
 LEFT JOIN "OpcionRespuesta" o ON (o."idPregunta",o."idPlantilla")=(i."idPregunta",a.opcion);
INSERT INTO "RespuestaCandidato" ("idCuestionario","ordenGlobal","idPlantilla","idBloqueCuestionario","idPregunta","idOpcionRespuesta")
 SELECT r.cuestionario,r.item,r.opcion,i."idBloqueCuestionario",i."idPregunta",a."idOpcionRespuesta" FROM respuesta r
 JOIN incluye i ON (i."idCuestionario",i."ordenGlobal")=(r.cuestionario,r.item)
 JOIN "AlternativaEmitida" a ON (a."idCuestionario",a."ordenGlobal",a."idPlantilla")=(r.cuestionario,r.item,r.opcion);
DO $$
DECLARE par text[]; anterior bigint; nuevo bigint;
BEGIN
 FOREACH par SLICE 1 IN ARRAY ARRAY[
 ['empresa','Empresa'],['puesto','Puesto'],['competencia','Competencia'],['requisito','requiere'],['factor','Factor'],
 ['pregunta','Pregunta'],['opcion','PlantillaOpcion'],['candidato','Candidato'],['auditoria','RegistroAuditoria'],
 ['evaluacion','Evaluacion'],['cuestionario','Cuestionario'],['item','incluye'],['alternativa','AlternativaEmitida'],['respuesta','RespuestaCandidato']
 ] LOOP
  EXECUTE format('SELECT count(*) FROM %I',par[1]) INTO anterior;
  EXECUTE format('SELECT count(*) FROM %I',par[2]) INTO nuevo;
  IF anterior<>nuevo THEN RAISE EXCEPTION 'Migración incompleta en %',par[2]; END IF;
 END LOOP;
 IF EXISTS (SELECT pregunta,opcion,puntaje FROM pregunta_opcion EXCEPT SELECT "idPregunta","idPlantilla",puntaje FROM "OpcionRespuesta") THEN
  RAISE EXCEPTION 'Faltan ponderaciones de preguntas'; END IF;
 FOREACH par SLICE 1 IN ARRAY ARRAY[['Pregunta','idPregunta'],['Evaluacion','idEvaluacion'],['Cuestionario','idCuestionario'],['RegistroAuditoria','idRegistroAuditoria']] LOOP
  EXECUTE format('SELECT setval(pg_get_serial_sequence(%L,%L),coalesce(max(%I),1),count(*)>0) FROM %I',quote_ident(par[1]),par[2],par[2],par[1]);
 END LOOP;
END $$;
SELECT registrar_resultado("idCuestionario") FROM "Cuestionario" WHERE "idEstadoCuestionario"='COMPLETADO';
DROP TABLE respuesta,alternativa,item,cuestionario,evaluacion,candidato,requisito,puesto,empresa,pregunta_opcion,opcion,pregunta,factor,competencia,auditoria;
UPDATE version_bd SET version=2 WHERE version=1;
