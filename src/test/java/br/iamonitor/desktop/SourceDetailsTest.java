package br.iamonitor.desktop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceDetailsTest {
  @Test void detalhaPromptSkillECacheReportado(@TempDir Path temp) throws Exception {
    Path project = Files.createDirectory(temp.resolve("project"));
    Path skill = project.resolve("skills/demo/SKILL.md");
    Files.createDirectories(skill.getParent());
    Files.writeString(skill, "# Skill de demonstração\nLeia os eventos.");
    try (Store store = new Store(temp.resolve("data"), new EventLogic())) {
      Store.Run run = store.begin(project, "Use skills/demo/SKILL.md no projeto.", "", "read-only");
      store.observeArtifacts(run.id(), "Leia \"skills/demo/SKILL.md\"", project);
      var mapper = new ObjectMapper();
      var completed = mapper.readTree("{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":120,\"cached_input_tokens\":30,\"output_tokens\":20,\"total_tokens\":140}}");
      store.recordEvent(run.id(), completed, mapper.writeValueAsString(completed));
      Store.Detail detail = store.detail(run.id());
      String prompt = SourceDetails.describe(store, detail, "prompt");
      assertTrue(prompt.contains("Use skills/demo/SKILL.md no projeto."));
      String skills = SourceDetails.describe(store, detail, "skill");
      assertTrue(skills.contains("demo"));
      assertTrue(skills.contains("skills\\demo\\SKILL.md") || skills.contains("skills/demo/SKILL.md"));
      assertTrue(skills.contains("# Skill de demonstração"));
      Store.Detail categories = new Store.Detail(detail.run(), List.of(), List.of(
          new Store.Artifact(skill.toString(), "Skill", "agora", 200, 50, ""),
          new Store.Artifact(project.resolve("specs/demo.md").toString(), "Spec", "agora", 300, 90, ""),
          new Store.Artifact("arquivo-solto.txt", "", "agora", 0, 0, "")),
          List.of(), Map.of(), true);
      String skillCategory = SourceDetails.describeCategory(categories, "Skill");
      assertTrue(skillCategory.contains("skills\\demo\\SKILL.md")
          || skillCategory.contains("skills/demo/SKILL.md"));
      assertTrue(skillCategory.contains("50 tokens"));
      assertFalse(skillCategory.contains("specs"));
      assertTrue(SourceDetails.describeCategory(categories, "Spec").contains("specs"));
      assertFalse(SourceDetails.describeCategory(categories, "OpenSpec").contains("demo.md"));
      assertTrue(SourceDetails.describeCategory(categories, "OpenSpec").contains("Nenhum arquivo"));
      assertTrue(SourceDetails.describeCategory(categories, "Arquivo").contains("arquivo-solto.txt"));
      String stagePrompt = StageDetails.describe(store, detail, 1);
      assertTrue(stagePrompt.equals("Use skills/demo/SKILL.md no projeto."));
      String stageContext = StageDetails.describe(store, detail, 3);
      assertTrue(stageContext.contains("skills"));
      assertTrue(stageContext.contains("# Skill de demonstração"));
      assertFalse(stageContext.contains("Nenhum arquivo"));
      assertFalse(stageContext.contains("Estimativas de arquivos"));
      assertFalse(stageContext.contains("SPEC"));
      String stageInput = StageDetails.describe(store, detail, 4);
      assertTrue(stageInput.contains("Entrada: 120"));
      assertTrue(stageInput.contains("Cache na entrada: 30"));
      assertFalse(stageInput.contains("A CLI não"));
      String stageCache = StageDetails.describe(store, detail, 9);
      assertTrue(stageCache.contains("Cache na entrada: 30"));
      assertFalse(stageCache.contains("contexto que o modelo"));
      String cache = SourceDetails.describe(store, detail, "cache");
      assertTrue(cache.contains("30 tokens em cache"));
      assertTrue(cache.contains("IA recebeu (entrada): 120"));
      assertTrue(cache.contains("Em cache: 30"));
      Files.writeString(skill, "Conteúdo alterado depois da coleta.");
      String changed = SourceDetails.describe(store, detail, "skill");
      assertTrue(changed.contains("# Skill de demonstração"));
      assertFalse(changed.contains("Conteúdo alterado depois da coleta."));
    }
  }
}
