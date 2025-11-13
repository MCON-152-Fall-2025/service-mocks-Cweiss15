package com.mcon152.recipeshare.service;

import com.mcon152.recipeshare.Recipe;
import com.mcon152.recipeshare.repository.RecipeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Assignment: Implement all TODOs using Mockito features covered in class:
 *  - @Mock, @InjectMocks, @Captor, @ExtendWith(MockitoExtension.class)
 *  - Stubbing: thenReturn / thenAnswer / thenThrow
 *  - Verifications: verify(...), times/never/atLeast..., verifyNoMoreInteractions
 *  - InOrder (where meaningful)
 *  - Void stubbing: doNothing / doThrow (use deleteById for this)
 *  - Matchers: any(), eq(), argThat()
 *  - ArgumentCaptor
 *  - (Optional) Spy demo if you introduce a small helper in tests
 *
 * NOTE: This is a pure unit test. Do NOT start a Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService (Mockito) — Assignment Skeleton")
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService; // CUT implements RecipeService

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    // --- Helpers for sample data ---

    private Recipe newRecipeNoId() {
        return new Recipe(
                null,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    private Recipe savedRecipe(long id) {
        return new Recipe(
                id,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    // ------------------ addRecipe ------------------

    @Nested
    @DisplayName("addRecipe(Recipe)")
    class AddRecipe {

        @Test
        @DisplayName("returns saved entity (thenReturn) and calls repository.save once")
        void returnsSaved_andSavesOnce() {
            // 1) when(recipeRepository.save(...)).thenReturn(savedRecipe(1L))
            // 2) call recipeService.addRecipe(newRecipeNoId())
            // 3) assert non-null id and fields
            // 4) verify(recipeRepository).save(any(Recipe.class)); verifyNoMoreInteractions(recipeRepository)

            //See code below as an example answer

            Recipe input = newRecipeNoId();
            Recipe saved = savedRecipe(1L);

            when(recipeRepository.save(any(Recipe.class))).thenReturn(saved);

            Recipe out = recipeService.addRecipe(input);
            assertEquals(1L, out.getId());
            assertEquals(saved, out);

            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("assigns ID dynamically (thenAnswer) and captures argument")
        void assignsId_thenAnswer_andCaptures() {
            // 1) Use thenAnswer to return a new Recipe with id=1L, copying fields from arg
            // 2) capture the arg with ArgumentCaptor and assert title, id==null pre-save

            //See code below as an example answer

            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
                Recipe r = inv.getArgument(0);
                return new Recipe(1L, r.getTitle(), r.getDescription(),
                        r.getIngredients(), r.getInstructions(), r.getServings());
            });

            Recipe out = recipeService.addRecipe(newRecipeNoId());
            assertEquals(1L, out.getId());

            verify(recipeRepository).save(recipeCaptor.capture());
            Recipe sent = recipeCaptor.getValue();
            assertNull(sent.getId()); // before persistence
            assertEquals("Chocolate Cake", sent.getTitle());
        }

        @Test
        @DisplayName("propagates repository failure (thenThrow)")
        void propagatesRepositoryFailure() {
            Recipe input = newRecipeNoId();
            when(recipeRepository.save(any())).thenThrow(new IllegalStateException("DB down"));
            assertThrows(IllegalStateException.class, () -> recipeService.addRecipe(input));

        }
    }

    // ------------------ getAllRecipes ------------------

    @Nested
    @DisplayName("getAllRecipes()")
    class GetAllRecipes {

        @Test
        @DisplayName("returns list from repository")
        void returnsList() {
            List<Recipe> recipes = List.of(savedRecipe(1L));
            when(recipeRepository.findAll()).thenReturn(recipes);
            List<Recipe> result = recipeService.getAllRecipes();
            assertSame(recipes, result);
            verify(recipeRepository).findAll();
        }
    }

    // ------------------ getRecipeById ------------------

    @Nested
    @DisplayName("getRecipeById(long)")
    class GetById {

        @Test
        @DisplayName("returns Optional.present when found")
        void present() {
            Recipe recipe = new Recipe(1L, "Challah", "tasty", "eggs, flour, water", "knead and rise and bake", 5);
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            assertTrue(recipeService.getRecipeById(1L).isPresent());

        }

        @Test
        @DisplayName("returns Optional.empty when missing")
        void empty() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());
            assertTrue(recipeService.getRecipeById(1L).isEmpty());

        }
    }

    // ------------------ deleteRecipe ------------------

    @Nested
    @DisplayName("deleteRecipe(long)")
    class DeleteRecipe {

        @Test
        @DisplayName("returns true when entity existed")
        void returnsTrue_whenExists() {
            Recipe recipe = new Recipe(1L, "Challah", "tasty", "eggs, flour, water", "knead and rise and bake", 5);
            Long id = recipe.getId();
            when(recipeRepository.existsById(id)).thenReturn(true);
            // Removed dead code: Optional<Recipe> exists = recipeService.getRecipeById(1L);
            doNothing().when(recipeRepository).deleteById(id);
            assertTrue(recipeService.deleteRecipe(1L));
            InOrder ordered = inOrder(recipeRepository);
            ordered.verify(recipeRepository).existsById(id);
            ordered.verify(recipeRepository).deleteById(id);
        }

        @Test
        @DisplayName("returns false when missing (never deletes)")
        void returnsFalse_whenMissing() {
            when(recipeRepository.existsById(1L)).thenReturn(false);
            assertFalse(recipeService.deleteRecipe(1L));
            verify(recipeRepository, never()).deleteById(1L);
            // existsById -> false; assert false; verify deleteById never called

        }

        @Test
        @DisplayName("propagates delete error (doThrow)")
        void propagatesDeleteError() {
            //existsById -> true; doThrow(...) on deleteById; assertThrows
            when(recipeRepository.existsById(1L)).thenReturn(true);
            doThrow(IllegalStateException.class).when(recipeRepository).deleteById(1L);
            assertThrows(IllegalStateException.class, () -> recipeService.deleteRecipe(1L));
        }
    }

    // ------------------ updateRecipe ------------------

    @Nested
    @DisplayName("updateRecipe(long, Recipe)")
    class UpdateRecipe {

        @Test
        @DisplayName("returns updated entity when exists")
        void returnsUpdated_whenExists() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(savedRecipe(1L)));
            Recipe updated = savedRecipe(1L);
            when(recipeRepository.save(any(Recipe.class))).thenReturn(updated);
            Optional<Recipe> result = recipeService.updateRecipe(1L, updated);
            assertEquals(updated, result.get());
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());
            Recipe updated = savedRecipe(1L);
            Optional<Recipe> result = recipeService.updateRecipe(1L, updated);
            assertTrue(result.isEmpty());
            verify(recipeRepository, never()).save(any(Recipe.class));
        }
    }

    // ------------------ patchRecipe ------------------

    @Nested
    @DisplayName("patchRecipe(long, Recipe)")
    class PatchRecipe {

        @Test
        @DisplayName("applies only non-null fields (argThat)")
        void appliesNonNullFields_only() {
            Recipe recipe = new Recipe(1L, "Challah", "tasty", "eggs, flour, water", "knead and rise and bake", 5);
            recipe.setDescription("Scrumptious");
            when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
            Recipe update = new Recipe();
            update.setDescription("Scrumptious");
            when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
            Optional<Recipe> result = recipeService.patchRecipe(1L, update);
            verify(recipeRepository).save(argThat((Recipe saved) -> saved.getDescription().equals("Scrumptious")));
            verify(recipeRepository).save(argThat((Recipe saved) -> saved.getTitle().equals("Challah")));
            assertEquals(recipe.getDescription(), result.get().getDescription());
            // findById -> present(existing)
            // provide partial with only title set
            // repository.save returns the modified entity (use thenAnswer echo)
            // verify save(argThat(...)) to ensure unchanged fields remain as-is
        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            when(recipeRepository.findById(1L)).thenReturn(Optional.empty());
            Recipe updated = savedRecipe(1L);
            Optional<Recipe> result = recipeService.patchRecipe(1L, updated);
            assertTrue(result.isEmpty());
            verify(recipeRepository, never()).save(any(Recipe.class));
        }
    }


    // ------------------ extra practice ------------------

    @Nested
    @DisplayName("Advanced stubbing & verification")
    class Advanced {

        @Test
        @DisplayName("consecutive stubs on existsById (true, false)")
        void consecutiveStubs_existsById() {
            when(recipeRepository.existsById(1L)).thenReturn(true, false);
            boolean one = recipeRepository.existsById(1L);
            boolean two = recipeRepository.existsById(1L);
            assertTrue(one);
            assertFalse(two);
            verify(recipeRepository, times(2)).existsById(1L);
            verifyNoMoreInteractions(recipeRepository);

        }
    }
}
