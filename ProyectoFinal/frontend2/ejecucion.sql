-- Puntajes de las alternativas efectivamente emitidas al candidato (CU026 y CU028).
CREATE VIEW puntaje_por_factor AS
 SELECT r."idCuestionario",i."idFactor",f."idCompetencia",avg(a.puntaje) AS puntaje,count(*) AS cantidad
 FROM "RespuestaCandidato" r JOIN incluye i USING ("idCuestionario","ordenGlobal")
 JOIN "Factor" f USING ("idFactor") JOIN "AlternativaEmitida" a USING ("idCuestionario","ordenGlobal","idPlantilla")
 GROUP BY r."idCuestionario",i."idFactor",f."idCompetencia";
CREATE FUNCTION registrar_resultado(cuestionario_id bigint) RETURNS void LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
DECLARE resultado_id bigint;
BEGIN
 INSERT INTO "Resultado" ("idCuestionario","puntajeObtenido","puntajeRequerido",activo,cumplimiento,"fechaCalculo")
 SELECT cuestionario_id,avg(c.puntaje),avg(c.minimo),true,bool_and(c.puntaje>=c.minimo),max(c.fin)
 FROM (SELECT req."idCompetencia",req."puntajeMinimo" AS minimo,q."fechaFin" AS fin,
       coalesce(avg(f.puntaje),0) AS puntaje
       FROM "Cuestionario" q JOIN "Evaluacion" e USING ("idEvaluacion") JOIN requiere req USING(codigo)
       LEFT JOIN puntaje_por_factor f ON f."idCuestionario"=q."idCuestionario" AND f."idCompetencia"=req."idCompetencia"
       WHERE q."idCuestionario"=cuestionario_id GROUP BY req."idCompetencia",req."puntajeMinimo",q."fechaFin") c
 HAVING count(*)>0 RETURNING "idResultado" INTO resultado_id;
 INSERT INTO "ResultadoCompetencia" ("idResultado","idCompetencia","puntajeObtenido","cumpleMinimo")
 SELECT resultado_id,req."idCompetencia",coalesce(avg(f.puntaje),0),
 coalesce(avg(f.puntaje),0)>=req."puntajeMinimo"
 FROM "Cuestionario" q JOIN "Evaluacion" e USING ("idEvaluacion") JOIN requiere req USING(codigo)
 LEFT JOIN puntaje_por_factor f ON f."idCuestionario"=q."idCuestionario" AND f."idCompetencia"=req."idCompetencia"
 WHERE q."idCuestionario"=cuestionario_id GROUP BY req."idCompetencia",req."puntajeMinimo";
 INSERT INTO "ResultadoFactor" ("idResultadoCompetencia","idFactor","puntajeObtenido")
 SELECT c."idResultadoCompetencia",f."idFactor",f.puntaje FROM "ResultadoCompetencia" c
 JOIN puntaje_por_factor f USING ("idCompetencia") WHERE c."idResultado"=resultado_id AND f."idCuestionario"=cuestionario_id;
END $$;
CREATE FUNCTION registrar_avance() RETURNS trigger LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
DECLARE instante bigint := (extract(epoch FROM clock_timestamp())*1000)::bigint;
BEGIN
 IF NEW."idEstadoCuestionario"<>OLD."idEstadoCuestionario" THEN NEW."fechaCambioEstado":=instante; END IF;
 IF NEW.posicion>OLD.posicion THEN
  UPDATE "BloqueCuestionario" SET completado=true,"fechaCompletado"=instante WHERE "idCuestionario"=NEW."idCuestionario"
    AND numero=(OLD.posicion/NEW."cantPreguntasPorBloque")+1;
 END IF;
 INSERT INTO "RegistroEjecucion" ("idCuestionario",accion,"fechaHora",detalle)
 VALUES(NEW."idCuestionario",CASE WHEN NEW.posicion>OLD.posicion THEN 'Confirmar bloque' WHEN NEW."cantAccesos">OLD."cantAccesos" THEN 'Ingresar' ELSE 'Cambiar estado' END,instante,NEW."idEstadoCuestionario");
 RETURN NEW;
END $$;
CREATE TRIGGER registrar_avance BEFORE UPDATE ON "Cuestionario" FOR EACH ROW EXECUTE FUNCTION registrar_avance();
CREATE FUNCTION finalizar_resultado() RETURNS trigger LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
BEGIN PERFORM registrar_resultado(NEW."idCuestionario"); RETURN NEW; END $$;
CREATE TRIGGER finalizar_resultado AFTER UPDATE ON "Cuestionario" FOR EACH ROW
 WHEN (NEW."idEstadoCuestionario"='COMPLETADO' AND OLD."idEstadoCuestionario"<>'COMPLETADO') EXECUTE FUNCTION finalizar_resultado();
