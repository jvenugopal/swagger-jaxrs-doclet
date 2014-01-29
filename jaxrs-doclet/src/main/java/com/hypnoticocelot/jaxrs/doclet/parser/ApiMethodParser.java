package com.hypnoticocelot.jaxrs.doclet.parser;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Collections2.filter;
import static com.hypnoticocelot.jaxrs.doclet.parser.AnnotationHelper.parsePath;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hypnoticocelot.jaxrs.doclet.DocletOptions;
import com.hypnoticocelot.jaxrs.doclet.model.ApiParameter;
import com.hypnoticocelot.jaxrs.doclet.model.ApiResponseMessage;
import com.hypnoticocelot.jaxrs.doclet.model.HttpMethod;
import com.hypnoticocelot.jaxrs.doclet.model.Method;
import com.hypnoticocelot.jaxrs.doclet.model.Model;
import com.hypnoticocelot.jaxrs.doclet.translator.Translator;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

public class ApiMethodParser {

    private final DocletOptions options;
    private final Translator translator;
    private final String parentPath;
    private final MethodDoc methodDoc;
    private final Set<Model> models;
    private final HttpMethod httpMethod;
    private final Method parentMethod;

    public ApiMethodParser(DocletOptions options, String parentPath, MethodDoc methodDoc) {
        this.options = options;
        this.translator = options.getTranslator();
        this.parentPath = parentPath;
        this.methodDoc = methodDoc;
        this.models = new LinkedHashSet<Model>();
        this.httpMethod = HttpMethod.fromMethod(methodDoc);
        this.parentMethod = null;
    }

    public ApiMethodParser(DocletOptions options, Method parentMethod, MethodDoc methodDoc) {
        this.options = options;
        this.translator = options.getTranslator();
        this.methodDoc = methodDoc;
        this.models = new LinkedHashSet<Model>();
        this.httpMethod = HttpMethod.fromMethod(methodDoc);
        this.parentPath = parentMethod.getPath();
        this.parentMethod = parentMethod;
    }

    public Method parse() {
        String methodPath = firstNonNull(parsePath(methodDoc.annotations()), "");
        if (httpMethod == null && methodPath.isEmpty()) {
            return null;
        }
        String path = parentPath + methodPath;

        // parameters
        List<ApiParameter> parameters = new LinkedList<ApiParameter>();
        for (Parameter parameter : methodDoc.parameters()) {
            if (!shouldIncludeParameter(httpMethod, parameter)) {
                continue;
            }
            if (options.isParseModels()) {
                models.addAll(new ApiModelParser(options, translator, parameter.type()).parse());
            }
            parameters.add(new ApiParameter(
                    AnnotationHelper.paramTypeOf(parameter),
                    AnnotationHelper.paramNameOf(parameter),
                    commentForParameter(methodDoc, parameter),
                    translator.typeName(parameter.type()).value()
            ));
        }

        // parent method parameters are inherited
        if (parentMethod != null)
            parameters.addAll(parentMethod.getParameters());

        // response messages
        Pattern pattern = Pattern.compile("(\\d+) (.+)"); // matches "<code><space><text>"
        List<ApiResponseMessage> responseMessages = new LinkedList<ApiResponseMessage>();
        for (String tagName : options.getErrorTags()) {
            for (Tag tagValue : methodDoc.tags(tagName)) {
                Matcher matcher = pattern.matcher(tagValue.text());
                if (matcher.find()) {
                    responseMessages.add(new ApiResponseMessage(Integer.valueOf(matcher.group(1)),
                            matcher.group(2)));
                }
            }
        }

        // return type
        Type type = methodDoc.returnType();
        String returnType = translator.typeName(type).value();
        if (options.isParseModels()) {
            models.addAll(new ApiModelParser(options, translator, type).parse());
        }

        // First Sentence of Javadoc method description
        Tag[] fst = methodDoc.firstSentenceTags();
        StringBuilder sentences = new StringBuilder();
        for (Tag tag : fst) {
            sentences.append(tag.text());
        }
        String firstSentences = sentences.toString();
	/**
	 * Changes made for removing pattern matching of Implementation Notes. 
	 */
        return new Method(
                httpMethod,
                methodDoc.name(),
                path,
                parameters,
                responseMessages,
                firstSentences,
                methodDoc.commentText().substring(firstSentences.length()),
                returnType
        );
    }

    public Set<Model> models() {
        return models;
    }

    private boolean shouldIncludeParameter(HttpMethod httpMethod, Parameter parameter) {
        List<AnnotationDesc> allAnnotations = Arrays.asList(parameter.annotations());
        Collection<AnnotationDesc> excluded = filter(allAnnotations, new AnnotationHelper.ExcludedAnnotations(options));
        if (!excluded.isEmpty()) {
            return false;
        }

        Collection<AnnotationDesc> jaxRsAnnotations = filter(allAnnotations, new AnnotationHelper.JaxRsAnnotations());
        if (!jaxRsAnnotations.isEmpty()) {
            return true;
        }

        return (allAnnotations.isEmpty() || httpMethod == HttpMethod.POST);
    }

    private String commentForParameter(MethodDoc method, Parameter parameter) {
        for (ParamTag tag : method.paramTags()) {
            if (tag.parameterName().equals(parameter.name())) {
                return tag.parameterComment();
            }
        }
        return "";
    }

}
