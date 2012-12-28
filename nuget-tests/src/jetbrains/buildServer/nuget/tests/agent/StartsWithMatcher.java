/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.nuget.tests.agent;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jetbrains.annotations.NotNull;

/**
* Created by Eugene Petrenko (eugene.petrenko@gmail.com)
* Date: 08.07.11 1:15
*/
public class StartsWithMatcher extends BaseMatcher<String> {
  private final String myPrefix;

  public StartsWithMatcher(@NotNull final String prefix) {
    myPrefix = prefix;
  }

  public boolean matches(Object o) {
    return o instanceof String && ((String) o).startsWith(myPrefix);
  }

  public void describeTo(Description description) {
    description.appendText("String starts with ").appendValue(myPrefix);
  }
}
