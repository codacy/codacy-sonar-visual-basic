using System.Xml.Linq;
using System.Collections.Generic;
using Codacy.Engine.Seed.Patterns;

namespace CodacyVisualBasic.DocsGenerator.Helpers
{
    public static class SubcategoryHelper
    {
        private static IDictionary<string, Subcategory> PatternToCategory { get; } = new Dictionary<string, Subcategory> {
            {"S4787", Subcategory.FileAccess},
            {"S4818", Subcategory.Cryptography},
            {"S4823", Subcategory.CommandInjection},
            {"S2255", Subcategory.Cryptography},
            {"S4784", Subcategory.CommandInjection}
        };

        public static Subcategory? ToSubcategory(XElement elem, Category category)
        {
            if (category != Category.Security)
            {
                return null;
            }

            var tags = elem.Elements("tag");
            foreach (var tag in tags)
            {
                switch (tag.Value)
                {
                    case "denial-of-service":
                        return Subcategory.DoS;
                    case "owasp-a1":
                        return Subcategory.CommandInjection;
                    case "owasp-a2":
                        return Subcategory.Auth;
                    case "owasp-a3":
                        return Subcategory.Cryptography;
                    case "owasp-a4":
                        return Subcategory.InputValidation;
                    case "owasp-a5":
                        return Subcategory.Auth;
                    case "owasp-a6":
                        return Subcategory.FileAccess;
                    case "owasp-a7":
                        return Subcategory.XSS;
                    case "owasp-a8":
                        return Subcategory.CommandInjection;
                    case "owasp-a9":
                        return Subcategory.InsecureModulesLibraries;
                    case "owasp-a10":
                        return Subcategory.Auth;
                    default:
                        continue;
                }
            }

            var elementName = elem.Element("key").Value;
            var foundPattern = PatternToCategory.TryGetValue(elementName, out Subcategory subcategory);
            return (foundPattern) ? subcategory : (Subcategory?)null;
        }
    }
}
